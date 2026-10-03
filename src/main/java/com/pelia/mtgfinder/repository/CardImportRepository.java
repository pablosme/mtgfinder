package com.pelia.mtgfinder.repository;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.postgresql.PGConnection;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.dto.ImportCardCounts;
import com.pelia.mtgfinder.dto.StagedCardRows;

@Repository
// Temporary tables, COPY, and write locks must use the service's transaction.
@Transactional(propagation = Propagation.MANDATORY)
public class CardImportRepository {

    private static final int CONTEXT_BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final CardImportCopyWriter copyWriter;

    public CardImportRepository(JdbcTemplate jdbcTemplate, CardImportCopyWriter copyWriter) {
        this.jdbcTemplate = jdbcTemplate;
        this.copyWriter = copyWriter;
    }

    public void stageBatch(List<StagedCardRows> rows) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            copy(connection, "card_oracle_staging", "import_id, source_card_print_id, "
                    + String.join(", ", CardImportColumns.ORACLE),
                    rows.stream().map(row -> copyWriter.writeOracle(row.oracle()))
                            .collect(Collectors.joining()), rows.size());
            copy(connection, "card_print_staging", String.join(", ", CardImportColumns.PRINT),
                    rows.stream().map(row -> copyWriter.writePrint(row.print()))
                            .collect(Collectors.joining()), rows.size());

            List<String> faces = rows.stream().flatMap(row -> row.faces().stream())
                    .map(copyWriter::writeFace)
                    .toList();
            if (!faces.isEmpty()) {
                copy(connection, "card_face_staging", "import_id, " + String.join(", ", CardImportColumns.FACE),
                        String.join("", faces), faces.size());
            }
            return null;
        });
    }

    public void lockProcessingImport(UUID importId) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            CardWriteLock.acquire(connection);
            return null;
        });

        List<String> statuses = jdbcTemplate.queryForList("""
                SELECT status FROM bulk_imports WHERE id = ? FOR UPDATE
                """, String.class, importId);
        if (statuses.isEmpty() || !"PROCESSING".equals(statuses.getFirst())) {
            throw new IllegalStateException("Import is not PROCESSING: " + importId);
        }
    }

    public ImportCardCounts countStagedCards(UUID importId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE live.id IS NULL) AS created,
                       count(*) FILTER (WHERE live.id IS NOT NULL
                           AND live.metadata_hash IS DISTINCT FROM staged.metadata_hash) AS updated
                FROM card_print_staging staged
                LEFT JOIN card_print live ON live.id = staged.id
                WHERE staged.import_id = ?
                """, (row, index) -> new ImportCardCounts(
                        row.getLong("total"),
                        Math.toIntExact(row.getLong("created")),
                        Math.toIntExact(row.getLong("updated"))), importId);
    }

    public void mergeOracles(UUID importId, List<String> contextFields) {
        String columns = String.join(", ", CardImportColumns.ORACLE);

        // Each identity can occur in many staged prints. Choose one consistently.
        jdbcTemplate.update("""
                CREATE TEMP TABLE incoming_card_oracle ON COMMIT DROP AS
                SELECT DISTINCT ON (id) %s
                FROM card_oracle_staging
                WHERE import_id = ?
                ORDER BY id, source_card_print_id
                """.formatted(columns), importId);

        List<String> contextColumns = contextFields.stream()
                .filter(CardImportColumns.ORACLE::contains)
                .toList();
        String incomingContext = contextColumns.stream().map(column -> "incoming." + column)
                .collect(Collectors.joining(", "));
        String storedContext = contextColumns.stream().map(column -> "stored." + column)
                .collect(Collectors.joining(", "));

        // Capture gameplay changes before overwriting the shared Oracle data.
        jdbcTemplate.execute("""
                CREATE TEMP TABLE changed_card_identities ON COMMIT DROP AS
                SELECT incoming.id
                FROM incoming_card_oracle incoming
                LEFT JOIN card_oracle stored ON stored.id = incoming.id
                WHERE stored.id IS NULL OR ROW(%s) IS DISTINCT FROM ROW(%s)
                """.formatted(incomingContext, storedContext));

        jdbcTemplate.update("""
                INSERT INTO card_oracle (%s)
                SELECT %s FROM incoming_card_oracle WHERE true
                ON CONFLICT (id) DO UPDATE SET %s
                """.formatted(columns, columns, assignments(CardImportColumns.ORACLE)));
    }

    public void mergePrintsAndFaces(UUID importId) {
        // Remove embeddings whose source identity/language would violate the FK after the merge.
        jdbcTemplate.update("""
                DELETE FROM card_embedding embedding USING card_print_staging staged
                WHERE staged.import_id = ? AND embedding.card_print_id = staged.id
                  AND (embedding.card_identity_id IS DISTINCT FROM staged.card_identity_id
                       OR embedding.lang IS DISTINCT FROM staged.lang)
                """, importId);

        String printColumns = String.join(", ", CardImportColumns.PRINT);
        jdbcTemplate.update("""
                INSERT INTO card_print (%s)
                SELECT %s FROM card_print_staging WHERE import_id = ?
                ON CONFLICT (id) DO UPDATE SET %s
                """.formatted(printColumns, printColumns, assignments(CardImportColumns.PRINT)), importId);

        jdbcTemplate.update("""
                DELETE FROM card_face WHERE card_print_id IN (
                    SELECT id FROM card_print_staging WHERE import_id = ?)
                """, importId);
        String faceColumns = String.join(", ", CardImportColumns.FACE);
        jdbcTemplate.update("""
                INSERT INTO card_face (%s)
                SELECT %s FROM card_face_staging WHERE import_id = ?
                """.formatted(faceColumns, faceColumns), importId);
    }

    public void refreshContexts(UUID importId, UnaryOperator<String> buildContext) {
        // Stream and update in batches so a full import does not have to fit in memory.
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var select = connection.prepareStatement("""
                    SELECT card.id, (to_jsonb(card) || to_jsonb(oracle) || jsonb_build_object('card_faces',
                        COALESCE((SELECT jsonb_agg(to_jsonb(face) ORDER BY face.face_index)
                                  FROM card_face face WHERE face.card_print_id = card.id), '[]'::jsonb)))::text AS data
                    FROM card_print card JOIN card_oracle oracle ON oracle.id = card.card_identity_id
                    WHERE card.import_id = ? OR card.card_identity_id IN (SELECT id FROM changed_card_identities)
                    """);
                    var update = connection.prepareStatement("""
                            UPDATE card_print SET context_text = ? WHERE id = ? AND context_text IS DISTINCT FROM ?
                            """)) {
                select.setObject(1, importId);
                select.setFetchSize(CONTEXT_BATCH_SIZE);
                int pending = 0;
                try (var rows = select.executeQuery()) {
                    while (rows.next()) {
                        String context = buildContext.apply(rows.getString("data"));
                        update.setString(1, context);
                        update.setObject(2, rows.getObject("id", UUID.class));
                        update.setString(3, context);
                        update.addBatch();
                        if (++pending == CONTEXT_BATCH_SIZE) {
                            update.executeBatch();
                            pending = 0;
                        }
                    }
                }
                if (pending > 0) {
                    update.executeBatch();
                }
            }
            return null;
        });
    }

    public void activateImport(UUID importId, ImportCardCounts counts) {
        jdbcTemplate.update("""
                UPDATE bulk_imports SET status = 'DEPRECATED' WHERE status = 'ACTIVE' AND id <> ?
                """, importId);
        int activated = jdbcTemplate.update("""
                UPDATE bulk_imports SET status = 'ACTIVE', total_cards = ?, created_cards = ?,
                    updated_cards = ?, activated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'PROCESSING'
                """, counts.total(), counts.created(), counts.updated(), importId);
        if (activated != 1) {
            throw new IllegalStateException("Import activation failed");
        }
    }

    public void deleteStagedRows(UUID importId) {
        for (String table : List.of("card_face_staging", "card_print_staging", "card_oracle_staging")) {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE import_id = ?", importId);
        }
    }

    public void deleteUnusedOracles() {
        jdbcTemplate.update("""
                DELETE FROM card_oracle oracle WHERE NOT EXISTS (
                    SELECT 1 FROM card_print card WHERE card.card_identity_id = oracle.id)
                """);
    }

    private void copy(Connection connection, String table, String columns, String rows, long expected)
            throws SQLException {
        try (var reader = new StringReader(rows)) {
            long copied = connection.unwrap(PGConnection.class).getCopyAPI()
                    .copyIn("COPY " + table + " (" + columns + ") FROM STDIN WITH (FORMAT text)", reader);
            if (copied != expected) {
                throw new IllegalStateException("COPY into %s returned %d rows; expected %d"
                        .formatted(table, copied, expected));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not copy rows into " + table, exception);
        }
    }

    private String assignments(List<String> columns) {
        return columns.stream()
                .filter(column -> !column.equals("id"))
                .map(column -> column + " = EXCLUDED." + column)
                .collect(Collectors.joining(", "));
    }
}
