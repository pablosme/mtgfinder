package com.pelia.mtgfinder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import com.pelia.mtgfinder.dto.ScryfallCard;

import tools.jackson.databind.ObjectMapper;

class PostgresBulkCopyServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataSource dataSource = mock(DataSource.class);
    private final PostgresBulkCopyService service = new PostgresBulkCopyService(dataSource, objectMapper);

    @Test
    void includesEmbeddingTextInTheImportedCardRow() {
        ScryfallCard card = card("""
                "name": "Fire // Ice",
                "layout": "split",
                "type_line": "Instant // Instant",
                "color_identity": ["U", "R"],
                "card_faces": [
                  {
                    "name": "Fire",
                    "mana_cost": "{1}{R}",
                    "type_line": "Instant",
                    "oracle_text": "Fire deals 2 damage divided as you choose."
                  }
                ]
                """);

        String cardRow = service.toCardCopyRow(UUID.randomUUID(), card);

        assertThat(cardRow.split("\t", -1)).hasSize(39);
        assertThat(cardRow)
                .contains("Name: Fire // Ice\\n")
                .contains("Face 1:\\n")
                .contains("Mana cost: {1}{R}\\n")
                .contains("Rules: Fire deals 2 damage divided as you choose.");
    }

    @Test
    void streamsCardRowsInSmallChunksWithCopyEscapingAndNullArrays() throws IOException {
        UUID importId = UUID.randomUUID();
        ScryfallCard firstCard = card("""
                "name": "Tab\\tLine\\nSlash\\\\End",
                "oracle_text": "First\\rSecond",
                "colors": [],
                "color_identity": ["U", "R"],
                "keywords": []
                """);
        ScryfallCard secondCard = card("""
                "name": "Second card",
                "color_identity": []
                """);

        String streamedRows = readInChunks(service.cardCopyReader(importId, List.of(firstCard, secondCard)), 7);
        String expectedRows = service.toCardCopyRow(importId, firstCard)
                + service.toCardCopyRow(importId, secondCard);
        String[] firstCardFields = service.toCardCopyRow(importId, firstCard).split("\t", -1);

        assertThat(streamedRows).isEqualTo(expectedRows);
        assertThat(streamedRows.lines()).hasSize(2);
        assertThat(firstCardFields).hasSize(39);
        assertThat(firstCardFields[3]).isEqualTo("Tab\\tLine\\nSlash\\\\End");
        assertThat(firstCardFields[6]).isEqualTo("\\N");
        assertThat(firstCardFields[14]).isEqualTo("First\\rSecond");
        assertThat(firstCardFields[15]).isEqualTo("{}");
        assertThat(firstCardFields[17]).isEqualTo("{}");
        assertThat(firstCardFields[18]).isEqualTo("\\N");
        assertThat(firstCardFields[33]).isEqualTo("{}");
        assertThat(firstCardFields[35]).contains("\"usd\":null", "\"eur\":null", "\"tix\":null");
    }

    @Test
    void streamsNoRowsForCardsWithoutFacesAndEveryRowForMultipleFaces() throws IOException {
        UUID importId = UUID.randomUUID();
        ScryfallCard cardWithoutFaces = card("""
                "name": "Single face",
                "color_identity": []
                """);
        ScryfallCard cardWithFaces = card("""
                "name": "Double card",
                "layout": "transform",
                "color_identity": [],
                "card_faces": [
                  {"name": "Front", "colors": []},
                  {"name": "Back", "colors": null}
                ]
                """);

        String noFaceRows = readInChunks(service.cardFaceCopyReader(importId, List.of(cardWithoutFaces)), 5);
        String streamedFaceRows = readInChunks(
                service.cardFaceCopyReader(importId, List.of(cardWithoutFaces, cardWithFaces)), 5);
        String expectedFaceRows = service.toCardFaceCopyRow(importId, cardWithFaces, 0)
                + service.toCardFaceCopyRow(importId, cardWithFaces, 1);

        assertThat(noFaceRows).isEmpty();
        assertThat(streamedFaceRows).isEqualTo(expectedFaceRows);
        assertThat(streamedFaceRows.lines()).hasSize(2);
        assertThat(service.toCardFaceCopyRow(importId, cardWithFaces, 0).split("\t", -1)).hasSize(17);
        assertThat(service.toCardFaceCopyRow(importId, cardWithFaces, 1)).contains("\\N");
    }

    @Test
    void rollsBackTheBatchWhenTheCardCopyFails() throws Exception {
        Connection connection = mock(Connection.class);
        PGConnection pgConnection = mock(PGConnection.class);
        CopyManager copyManager = mock(CopyManager.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
        when(pgConnection.getCopyAPI()).thenReturn(copyManager);
        when(copyManager.copyIn(anyString(), any(Reader.class)))
                .thenThrow(new SQLException("card COPY failed"));
        ScryfallCard card = card("""
                "name": "Single face",
                "color_identity": []
                """);

        assertThatThrownBy(() -> service.insertBatchCopy(UUID.randomUUID(), List.of(card)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error during batch copy insert")
                .hasRootCauseMessage("card COPY failed");

        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void rollsBackTheBatchWhenTheCardFaceCopyFails() throws Exception {
        Connection connection = mock(Connection.class);
        PGConnection pgConnection = mock(PGConnection.class);
        CopyManager copyManager = mock(CopyManager.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
        when(pgConnection.getCopyAPI()).thenReturn(copyManager);
        when(copyManager.copyIn(anyString(), any(Reader.class)))
                .thenReturn(1L)
                .thenThrow(new SQLException("card face COPY failed"));
        ScryfallCard card = card("""
                "name": "Double card",
                "layout": "transform",
                "color_identity": [],
                "card_faces": [{"name": "Front", "colors": []}]
                """);

        assertThatThrownBy(() -> service.insertBatchCopy(UUID.randomUUID(), List.of(card)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error during batch copy insert")
                .hasRootCauseMessage("card face COPY failed");

        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    private ScryfallCard card(String fields) {
        String json = """
                {
                  "id": "%s",
                  "lang": "en",
                  "layout": "normal",
                  "set": "tst",
                  "set_name": "Test Set",
                  "collector_number": "1",
                  "rarity": "common",
                  "legalities": {},
                  "prices": {},
                  %s
                }
                """.formatted(UUID.randomUUID(), fields);
        return objectMapper.readValue(json, ScryfallCard.class);
    }

    private String readInChunks(Reader reader, int bufferSize) throws IOException {
        try (reader; StringWriter output = new StringWriter()) {
            char[] buffer = new char[bufferSize];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString();
        }
    }
}
