package com.pelia.mtgfinder.service;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.stereotype.Service;

import com.pelia.mtgfinder.dto.ScryfallCard;

import tools.jackson.databind.ObjectMapper;

@Service
public class PostgresBulkCopyService {
    private static final String DELETE_IMPORT_CARDS_SQL = "DELETE FROM cards WHERE import_id = ?";

    private static final String COPY_CARDS_SQL = """
            COPY cards (
                id, import_id, oracle_id, name, lang, layout, multiverse_ids,
                mtgo_id, arena_id, tcgplayer_id, cardmarket_id, mana_cost, cmc,
                type_line, oracle_text, colors, color_identity, keywords,
                produced_mana, power, toughness, loyalty, defense, reserved,
                digital, reprint, set_code, set_name, collector_number, rarity,
                flavor_text, artist, released_at, legalities, image_uris, prices,
                related_uris, all_parts, context_text
            ) FROM STDIN WITH (FORMAT text)
            """;

    private static final String COPY_CARD_FACES_SQL = """
            COPY card_faces (
                import_id, card_id, face_index, name, oracle_id, mana_cost, cmc,
                type_line, oracle_text, colors, power, toughness, loyalty,
                defense, flavor_text, artist, image_uris
            ) FROM STDIN WITH (FORMAT text)
            """;

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public PostgresBulkCopyService(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public void insertBatchCopy(UUID importId, List<ScryfallCard> cardRows) {
        Objects.requireNonNull(importId, "importId must not be null");
        Objects.requireNonNull(cardRows, "cardRows must not be null");

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                CopyManager copyManager = connection.unwrap(PGConnection.class).getCopyAPI();
                // TODO: Validar os totais retornados por copyIn contra as quantidades
                // esperadas de cards e card_faces.
                try (Reader cardReader = cardCopyReader(importId, cardRows)) {
                    copyManager.copyIn(COPY_CARDS_SQL, cardReader);
                }

                if (hasCardFaces(cardRows)) {
                    try (Reader cardFaceReader = cardFaceCopyReader(importId, cardRows)) {
                        copyManager.copyIn(COPY_CARD_FACES_SQL, cardFaceReader);
                    }
                }

                connection.commit();
            } catch (SQLException | IOException | RuntimeException exception) {
                rollback(connection, exception);
                // TODO: Acrescentar importId, tamanho do batch e SQLState ao erro/log.
                throw new IllegalStateException("Error during batch copy insert", exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not access PostgreSQL during batch copy", exception);
        }
    }

    public void deleteImportData(UUID importId) {
        Objects.requireNonNull(importId, "importId must not be null");

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(DELETE_IMPORT_CARDS_SQL)) {
                statement.setObject(1, importId);
                statement.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw new IllegalStateException("Could not delete data for failed import " + importId, exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not access PostgreSQL during failed import cleanup", exception);
        }
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    String toCardCopyRow(UUID importId, ScryfallCard card) {
        Objects.requireNonNull(importId, "importId must not be null");
        Objects.requireNonNull(card, "card must not be null");

        StringBuilder cardRow = new StringBuilder();
        cardRow.append(copyValue(card.id())).append("\t")
                .append(copyValue(importId)).append("\t")
                .append(copyValue(card.oracleId())).append("\t")
                .append(copyValue(card.name())).append("\t")
                .append(copyValue(card.lang())).append("\t")
                .append(copyValue(card.layout())).append("\t")
                .append(copyArray(card.multiverseIds())).append("\t")
                .append(copyValue(card.mtgoId())).append("\t")
                .append(copyValue(card.arenaId())).append("\t")
                .append(copyValue(card.tcgplayerId())).append("\t")
                .append(copyValue(card.cardmarketId())).append("\t")
                .append(copyValue(card.manaCost())).append("\t")
                .append(copyValue(card.cmc())).append("\t")
                .append(copyValue(card.typeLine())).append("\t")
                .append(copyValue(card.oracleText())).append("\t")
                .append(copyArray(card.colors())).append("\t")
                .append(copyArray(card.colorIdentity())).append("\t")
                .append(copyArray(card.keywords())).append("\t")
                .append(copyArray(card.producedMana())).append("\t")
                .append(copyValue(card.power())).append("\t")
                .append(copyValue(card.toughness())).append("\t")
                .append(copyValue(card.loyalty())).append("\t")
                .append(copyValue(card.defense())).append("\t")
                .append(copyValue(card.reserved())).append("\t")
                .append(copyValue(card.digital())).append("\t")
                .append(copyValue(card.reprint())).append("\t")
                .append(copyValue(card.set())).append("\t")
                .append(copyValue(card.setName())).append("\t")
                .append(copyValue(card.collectorNumber())).append("\t")
                .append(copyValue(card.rarity())).append("\t")
                .append(copyValue(card.flavorText())).append("\t")
                .append(copyValue(card.artist())).append("\t")
                .append(copyValue(card.releasedAt())).append("\t")
                .append(copyJson(card.legalities())).append("\t")
                .append(copyJson(card.imageUris())).append("\t")
                .append(copyJson(card.prices())).append("\t")
                .append(copyJson(card.relatedUris())).append("\t")
                .append(copyJson(card.allParts())).append("\t")
                .append(copyValue(buildEmbeddingText(card))).append("\n");

        return cardRow.toString();
    }

    String toCardFaceCopyRow(UUID importId, ScryfallCard card, int faceIndex) {
        Objects.requireNonNull(importId, "importId must not be null");
        Objects.requireNonNull(card, "card must not be null");
        if (card.cardFaces() == null || faceIndex < 0 || faceIndex >= card.cardFaces().size()) {
            throw new IndexOutOfBoundsException("Invalid card face index: " + faceIndex);
        }

        ScryfallCard.CardFace face = card.cardFaces().get(faceIndex);
        StringBuilder cardFaceRow = new StringBuilder();
        cardFaceRow.append(copyValue(importId)).append("\t")
                .append(copyValue(card.id())).append("\t")
                .append(copyValue(faceIndex)).append("\t")
                .append(copyValue(face.name())).append("\t")
                .append(copyValue(face.oracleId())).append("\t")
                .append(copyValue(face.manaCost())).append("\t")
                .append(copyValue(face.cmc())).append("\t")
                .append(copyValue(face.typeLine())).append("\t")
                .append(copyValue(face.oracleText())).append("\t")
                .append(copyArray(face.colors())).append("\t")
                .append(copyValue(face.power())).append("\t")
                .append(copyValue(face.toughness())).append("\t")
                .append(copyValue(face.loyalty())).append("\t")
                .append(copyValue(face.defense())).append("\t")
                .append(copyValue(face.flavorText())).append("\t")
                .append(copyValue(face.artist())).append("\t")
                .append(copyJson(face.imageUris())).append("\n");

        return cardFaceRow.toString();
    }

    Reader cardCopyReader(UUID importId, List<ScryfallCard> cards) {
        Iterator<ScryfallCard> cardIterator = cards.iterator();
        return new CopyRowReader(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return cardIterator.hasNext();
            }

            @Override
            public String next() {
                return toCardCopyRow(importId, cardIterator.next());
            }
        });
    }

    Reader cardFaceCopyReader(UUID importId, List<ScryfallCard> cards) {
        return new CopyRowReader(new CardFaceRowIterator(importId, cards));
    }

    private boolean hasCardFaces(List<ScryfallCard> cards) {
        return cards.stream().anyMatch(card -> card.cardFaces() != null && !card.cardFaces().isEmpty());
    }

    private final class CardFaceRowIterator implements Iterator<String> {
        private final UUID importId;
        private final List<ScryfallCard> cards;
        private int cardIndex;
        private int faceIndex;

        private CardFaceRowIterator(UUID importId, List<ScryfallCard> cards) {
            this.importId = importId;
            this.cards = cards;
        }

        @Override
        public boolean hasNext() {
            while (cardIndex < cards.size()) {
                List<ScryfallCard.CardFace> faces = cards.get(cardIndex).cardFaces();
                if (faces != null && faceIndex < faces.size()) {
                    return true;
                }
                cardIndex++;
                faceIndex = 0;
            }
            return false;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            ScryfallCard card = cards.get(cardIndex);
            return toCardFaceCopyRow(importId, card, faceIndex++);
        }
    }

    private static final class CopyRowReader extends Reader {
        private Iterator<String> rows;
        private String currentRow;
        private int currentOffset;
        private boolean closed;

        private CopyRowReader(Iterator<String> rows) {
            this.rows = rows;
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, buffer.length);
            ensureOpen();
            if (length == 0) {
                return 0;
            }

            int charactersRead = 0;
            while (charactersRead < length) {
                if (currentRow == null || currentOffset >= currentRow.length()) {
                    if (!rows.hasNext()) {
                        return charactersRead == 0 ? -1 : charactersRead;
                    }
                    currentRow = Objects.requireNonNull(rows.next(), "COPY row must not be null");
                    currentOffset = 0;
                }

                int chunkLength = Math.min(length - charactersRead, currentRow.length() - currentOffset);
                currentRow.getChars(currentOffset, currentOffset + chunkLength, buffer, offset + charactersRead);
                currentOffset += chunkLength;
                charactersRead += chunkLength;

                if (currentOffset == currentRow.length()) {
                    currentRow = null;
                }
            }
            return charactersRead;
        }

        @Override
        public void close() {
            closed = true;
            currentRow = null;
            rows = List.<String>of().iterator();
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Reader is closed");
            }
        }
    }

    private String buildEmbeddingText(ScryfallCard card) {
        StringBuilder context = new StringBuilder();
        appendContext(context, "Name", card.name());
        appendContext(context, "Printed name", card.printedName());
        appendContext(context, "Language", card.lang());
        appendContext(context, "Mana cost", card.manaCost());
        appendContext(context, "Type", card.typeLine());
        appendContext(context, "Printed type", card.printedTypeLine());
        appendContext(context, "Rules", card.oracleText());
        appendContext(context, "Printed rules", card.printedText());
        appendContext(context, "Colors", card.colors());
        appendContext(context, "Color identity", card.colorIdentity());
        appendContext(context, "Keywords", card.keywords());
        appendContext(context, "Produced mana", card.producedMana());
        appendContext(context, "Power", card.power());
        appendContext(context, "Toughness", card.toughness());
        appendContext(context, "Loyalty", card.loyalty());
        appendContext(context, "Defense", card.defense());
        appendContext(context, "Set", card.setName());
        appendContext(context, "Rarity", card.rarity());
        appendContext(context, "Flavor text", card.flavorText());

        if (card.cardFaces() != null) {
            for (int index = 0; index < card.cardFaces().size(); index++) {
                ScryfallCard.CardFace face = card.cardFaces().get(index);
                context.append("Face ").append(index + 1).append(':').append('\n');
                appendContext(context, "Name", face.name());
                appendContext(context, "Printed name", face.printedName());
                appendContext(context, "Mana cost", face.manaCost());
                appendContext(context, "Type", face.typeLine());
                appendContext(context, "Printed type", face.printedTypeLine());
                appendContext(context, "Rules", face.oracleText());
                appendContext(context, "Printed rules", face.printedText());
                appendContext(context, "Colors", face.colors());
                appendContext(context, "Power", face.power());
                appendContext(context, "Toughness", face.toughness());
                appendContext(context, "Loyalty", face.loyalty());
                appendContext(context, "Defense", face.defense());
                appendContext(context, "Flavor text", face.flavorText());
            }
        }

        return context.toString().stripTrailing();
    }

    private void appendContext(StringBuilder context, String label, Object value) {
        if (value == null) {
            return;
        }

        String text = value instanceof Collection<?> values
                ? values.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .filter(item -> !item.isBlank())
                        .collect(Collectors.joining(", "))
                : value.toString().strip();
        if (!text.isBlank()) {
            context.append(label).append(": ").append(text).append('\n');
        }
    }

    private String copyValue(Object value) {
        return value == null ? "\\N" : escapeCopyText(value.toString());
    }

    private String copyJson(Object value) {
        return value == null ? "\\N" : escapeCopyText(objectMapper.writeValueAsString(value));
    }

    private String copyArray(List<?> values) {
        if (values == null) {
            return "\\N";
        }

        StringBuilder array = new StringBuilder("{");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                array.append(',');
            }

            Object value = values.get(index);
            if (value == null) {
                array.append("NULL");
            } else {
                array.append('"')
                        .append(value.toString().replace("\\", "\\\\").replace("\"", "\\\""))
                        .append('"');
            }
        }
        array.append('}');
        return escapeCopyText(array.toString());
    }

    private String escapeCopyText(String value) {
        return value.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
