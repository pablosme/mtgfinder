package com.pelia.mtgfinder.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.model.Card;

@SpringBootTest
@Transactional
class CardRepositoryTokenBatchTest {

    @Autowired
    private CardRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectsTheOrderedPrefixThatFitsTheEstimatedTokenBudget() {
        UUID importId = UUID.randomUUID();
        UUID firstCardId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondCardId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID thirdCardId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        jdbcTemplate.update(
                "INSERT INTO bulk_imports (id, scryfall_bulk_type) VALUES (?, ?)",
                importId, "test-cards");
        insertCard(importId, firstCardId, "one two");
        insertCard(importId, secondCardId, "one two three");
        insertCard(importId, thirdCardId, "one");

        List<Card> cardsWithinBudget = repository.findPendingEmbeddingsByImportId(importId, 8);
        List<Card> oversizedFirstCard = repository.findPendingEmbeddingsByImportId(importId, 1);

        assertThat(cardsWithinBudget)
                .extracting(card -> card.getId().getId())
                .containsExactly(firstCardId, secondCardId);
        assertThat(oversizedFirstCard)
                .extracting(card -> card.getId().getId())
                .containsExactly(firstCardId);
    }

    private void insertCard(UUID importId, UUID cardId, String contextText) {
        jdbcTemplate.update("""
                INSERT INTO cards (
                    id, import_id, name, lang, layout, color_identity, keywords,
                    reserved, digital, reprint, set_code, set_name,
                    collector_number, rarity, legalities, prices, context_text
                ) VALUES (
                    ?, ?, ?, 'en', 'normal', '{}'::varchar(5)[], '{}'::text[],
                    FALSE, FALSE, FALSE, 'tst', 'Test Set',
                    '1', 'common', '{}'::jsonb, '{}'::jsonb, ?
                )
                """, cardId, importId, "Test card " + cardId, contextText);
    }
}
