package com.pelia.mtgfinder.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.pelia.mtgfinder.dto.PendingCardEmbedding;

@Repository
public class CardEmbeddingRepository {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public CardEmbeddingRepository(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PendingCardEmbedding> findPending(int limit) {
        return jdbcTemplate.query("""
                WITH missing AS (
                    SELECT DISTINCT ON (card.card_identity_id, card.lang)
                           card.card_identity_id, card.lang, card.id AS card_print_id, card.context_text
                    FROM card_print card
                    WHERE NOT EXISTS (SELECT 1 FROM card_embedding embedding
                                      WHERE embedding.card_identity_id = card.card_identity_id
                                        AND embedding.lang = card.lang)
                    ORDER BY card.card_identity_id, card.lang, card.id
                ), pending AS (
                    SELECT * FROM missing
                    UNION ALL
                    SELECT embedding.card_identity_id, embedding.lang, card.id, card.context_text
                    FROM card_embedding embedding
                    JOIN card_print card ON card.id = embedding.card_print_id
                    WHERE card.context_text IS DISTINCT FROM embedding.context_text
                )
                SELECT * FROM pending ORDER BY card_identity_id, lang LIMIT ?
                """, (row, index) -> new PendingCardEmbedding(
                        row.getObject("card_identity_id", UUID.class), row.getString("lang"),
                        row.getObject("card_print_id", UUID.class), row.getString("context_text")), limit);
    }

    public int saveBatch(List<PendingCardEmbedding> sources, List<float[]> vectors) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                CardWriteLock.acquire(connection);
                int saved = 0;
                // Recheck the source snapshot after inference. An import or another worker
                // may have changed it; never attach an old vector to newer text or a new source.
                try (var statement = connection.prepareStatement("""
                        INSERT INTO card_embedding (card_identity_id, lang, card_print_id, context_text, embedding)
                        SELECT card.card_identity_id, card.lang, card.id, ?, CAST(? AS vector(1024))
                        FROM card_print card
                        WHERE card.id = ? AND card.card_identity_id = ? AND card.lang = ?
                          AND card.context_text = ?
                        ON CONFLICT (card_identity_id, lang) DO UPDATE
                        SET context_text = EXCLUDED.context_text, embedding = EXCLUDED.embedding
                        WHERE card_embedding.card_print_id = EXCLUDED.card_print_id
                          AND card_embedding.context_text IS DISTINCT FROM EXCLUDED.context_text
                        """)) {
                    for (int index = 0; index < sources.size(); index++) {
                        PendingCardEmbedding source = sources.get(index);
                        statement.setString(1, source.contextText());
                        statement.setString(2, Arrays.toString(vectors.get(index)));
                        statement.setObject(3, source.cardPrintId());
                        statement.setObject(4, source.cardIdentityId());
                        statement.setString(5, source.lang());
                        statement.setString(6, source.contextText());
                        saved += statement.executeUpdate();
                    }
                }
                connection.commit();
                return saved;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw new IllegalStateException("Could not persist card embeddings", exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not access PostgreSQL during embedding persistence", exception);
        }
    }
}
