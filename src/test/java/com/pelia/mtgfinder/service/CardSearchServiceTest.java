package com.pelia.mtgfinder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import com.pelia.mtgfinder.dto.CardSearchResponse;
import com.pelia.mtgfinder.repository.ActiveCardRepository;
import com.pelia.mtgfinder.repository.CardFaceRepository;

import tools.jackson.databind.ObjectMapper;

class CardSearchServiceTest {

    private final ActiveCardRepository activeCardRepository = mock(ActiveCardRepository.class);
    private final CardFaceRepository cardFaceRepository = mock(CardFaceRepository.class);
    private final EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
    private final CardSearchService service = new CardSearchService(
            activeCardRepository,
            cardFaceRepository,
            new ObjectMapper(),
            embeddingModel,
            10);

    @Test
    void searchesActiveCardsUsingThePromptEmbedding() {
        when(embeddingModel.embed("draw cards")).thenReturn(new float[] { 0.1f, -0.2f });
        when(activeCardRepository.searchByEmbedding("[0.1, -0.2]", 10)).thenReturn(List.of());

        CardSearchResponse response = service.searchByPrompt("draw cards");

        assertThat(response.query()).isEqualTo("draw cards");
        assertThat(response.totalResults()).isZero();
        assertThat(response.data()).isEmpty();
        verify(activeCardRepository).searchByEmbedding("[0.1, -0.2]", 10);
    }
}
