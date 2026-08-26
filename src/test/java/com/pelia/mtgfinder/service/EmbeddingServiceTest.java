package com.pelia.mtgfinder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import com.pelia.mtgfinder.model.BulkImport;
import com.pelia.mtgfinder.model.Card;
import com.pelia.mtgfinder.model.CardId;
import com.pelia.mtgfinder.repository.CardRepository;

class EmbeddingServiceTest {

	private final EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
	private final BulkImportService bulkImportService = mock(BulkImportService.class);
	private final CardRepository cardRepository = mock(CardRepository.class);
	private final UUID importId = UUID.randomUUID();

	private EmbeddingService service;

	@BeforeEach
	void setUp() {
		service = new EmbeddingService(
				embeddingModel,
				bulkImportService,
				cardRepository,
				100,
				3);
		when(bulkImportService.findById(importId)).thenReturn(new BulkImport());
	}

	@Test
	void embedsTheContextTextPersistedDuringImport() {
		Card card = card("Fire // Ice");

		when(cardRepository.findPendingEmbeddingsByImportId(importId, 100))
				.thenReturn(List.of(card), List.of());
		when(embeddingModel.embed(anyList()))
				.thenReturn(Collections.singletonList(new float[] { 0.1f, 0.2f, 0.3f }));

		EmbeddingService.EmbeddingResult result = service.embedImport(importId);

		assertThat(result.importId()).isEqualTo(importId);
		assertThat(result.embeddedCards()).isEqualTo(1);
		verify(embeddingModel).embed(List.of(card.getContextText()));
		verify(cardRepository, org.mockito.Mockito.times(2))
				.findPendingEmbeddingsByImportId(importId, 100);
		assertThat(card.getEmbedding()).containsExactly(0.1f, 0.2f, 0.3f);
		verify(cardRepository).saveAllAndFlush(List.of(card));
	}

	@Test
	void returnsZeroWhenEveryCardWasAlreadyEmbedded() {
		when(cardRepository.findPendingEmbeddingsByImportId(importId, 100))
				.thenReturn(List.of());

		EmbeddingService.EmbeddingResult result = service.embedImport(importId);

		assertThat(result.embeddedCards()).isZero();
		verify(embeddingModel, never()).embed(anyList());
		verify(cardRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void rejectsVectorsWithUnexpectedDimensionsBeforeSaving() {
		Card card = card("Lightning Bolt");
		when(cardRepository.findPendingEmbeddingsByImportId(importId, 100))
				.thenReturn(List.of(card));
		when(embeddingModel.embed(anyList()))
				.thenReturn(Collections.singletonList(new float[] { 0.1f, 0.2f }));

		assertThatThrownBy(() -> service.embedImport(importId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Embedding model returned 2 dimensions; expected 3");
		verify(cardRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void rejectsCardsImportedWithoutContextText() {
		Card card = card("Lightning Bolt");
		card.setContextText(null);
		when(cardRepository.findPendingEmbeddingsByImportId(importId, 100))
				.thenReturn(List.of(card));

		assertThatThrownBy(() -> service.embedImport(importId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("does not have context text");
		verify(embeddingModel, never()).embed(anyList());
		verify(cardRepository, never()).saveAllAndFlush(anyList());
	}

	@Test
	void estimatesTokensUsingWordCountWithThirtyFivePercentMargin() {
		assertThat(service.estimateTokens("one two three four")).isEqualTo(6);
		assertThat(service.estimateTokens(" one\n\ttwo   three ")).isEqualTo(5);
	}

	@Test
	void rejectsAnIndividualCardAboveTheTokenBudgetBeforeCallingTheModel() {
		EmbeddingService limitedService = new EmbeddingService(
				embeddingModel,
				bulkImportService,
				cardRepository,
				5,
				3);
		Card card = card("Lightning Bolt");
		card.setContextText("one two three four");
		when(cardRepository.findPendingEmbeddingsByImportId(importId, 5))
				.thenReturn(List.of(card));

		assertThatThrownBy(() -> limitedService.embedImport(importId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is estimated at 6 tokens; configured maximum is 5");
		verify(embeddingModel, never()).embed(anyList());
		verify(cardRepository, never()).saveAllAndFlush(anyList());
	}

	private Card card(String name) {
		Card card = new Card();
		card.setId(new CardId(importId, UUID.randomUUID()));
		card.setName(name);
		card.setContextText("Name: " + name + "\nType: Instant\nRules: Choose one.");
		return card;
	}
}
