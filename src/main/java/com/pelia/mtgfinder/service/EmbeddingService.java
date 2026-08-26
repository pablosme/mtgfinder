package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pelia.mtgfinder.model.Card;
import com.pelia.mtgfinder.repository.CardRepository;

@Service
public class EmbeddingService {
	private final EmbeddingModel embeddingModel;
	private final BulkImportService bulkImportService;
	private final CardRepository cardRepository;
	private final int maxEstimatedTokens;
	private final int embeddingDimensions;

	public EmbeddingService(
			EmbeddingModel embeddingModel,
			BulkImportService bulkImportService,
			CardRepository cardRepository,
			@Value("${com.pelia.mtgfinder.embedding-max-tokens:8000}") int maxEstimatedTokens,
			@Value("${spring.ai.vectorstore.pgvector.dimensions}") int embeddingDimensions) {
		if (maxEstimatedTokens <= 0) {
			throw new IllegalArgumentException("Embedding maximum estimated tokens must be greater than zero");
		}
		if (embeddingDimensions <= 0) {
			throw new IllegalArgumentException("Embedding dimensions must be greater than zero");
		}

		this.embeddingModel = embeddingModel;
		this.bulkImportService = bulkImportService;
		this.cardRepository = cardRepository;
		this.maxEstimatedTokens = maxEstimatedTokens;
		this.embeddingDimensions = embeddingDimensions;
	}

	public EmbeddingResult embedImport(UUID importId) {
		if (importId == null) {
			throw new IllegalArgumentException("Import ID must not be null");
		}

		bulkImportService.findById(importId);

		int embeddedCards = 0;
		while (true) {
			List<Card> cards = cardRepository.findPendingEmbeddingsByImportId(
					importId, maxEstimatedTokens);
			if (cards.isEmpty()) {
				break;
			}

			List<String> contexts = cards.stream()
					.map(this::contextText)
					.toList();
			validateEstimatedTokens(cards, contexts);
			List<float[]> embeddings = embeddingModel.embed(contexts);

			validateEmbeddings(cards.size(), embeddings);
			for (int index = 0; index < cards.size(); index++) {
				cards.get(index).setEmbedding(embeddings.get(index));
			}

			cardRepository.saveAllAndFlush(cards);
			embeddedCards += cards.size();
		}

		return new EmbeddingResult(importId, embeddedCards);
	}

	private String contextText(Card card) {
		String contextText = card.getContextText();
		if (contextText == null || contextText.isBlank()) {
			throw new IllegalStateException(
					"Card %s from import %s does not have context text"
							.formatted(card.getId().getId(), card.getId().getImportId()));
		}
		return contextText;
	}

	private void validateEstimatedTokens(List<Card> cards, List<String> contexts) {
		long estimatedTokens = contexts.stream()
				.mapToLong(this::estimateTokens)
				.sum();
		if (estimatedTokens <= maxEstimatedTokens) {
			return;
		}

		if (cards.size() == 1) {
			Card card = cards.getFirst();
			throw new IllegalStateException(
					"Card %s from import %s is estimated at %d tokens; configured maximum is %d"
							.formatted(
									card.getId().getId(),
									card.getId().getImportId(),
									estimatedTokens,
									maxEstimatedTokens));
		}

		throw new IllegalStateException(
				"Embedding batch is estimated at %d tokens; configured maximum is %d"
						.formatted(estimatedTokens, maxEstimatedTokens));
	}

	int estimateTokens(String contextText) {
		int words = contextText.strip().split("\\s+").length;
		return (int) Math.ceil(words * 1.35d);
	}

	private void validateEmbeddings(int expectedCount, List<float[]> embeddings) {
		if (embeddings == null || embeddings.size() != expectedCount) {
			int actualCount = embeddings == null ? 0 : embeddings.size();
			throw new IllegalStateException(
					"Embedding model returned %d vectors for %d cards".formatted(actualCount, expectedCount));
		}

		for (float[] embedding : embeddings) {
			if (embedding == null || embedding.length != embeddingDimensions) {
				int actualDimensions = embedding == null ? 0 : embedding.length;
				throw new IllegalStateException(
						"Embedding model returned %d dimensions; expected %d"
								.formatted(actualDimensions, embeddingDimensions));
			}
		}
	}

	public record EmbeddingResult(UUID importId, int embeddedCards) {
	}
}
