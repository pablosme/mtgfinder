package com.pelia.mtgfinder.service;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pelia.mtgfinder.dto.EmbeddingResult;
import com.pelia.mtgfinder.model.Card;
import com.pelia.mtgfinder.repository.CardRepository;

@Service
public class EmbeddingService {
	private final EmbeddingModel embeddingModel;
	private final CardRepository cardRepository;
	private final int batchSize;
	private final int embeddingDimensions;

	public EmbeddingService(
			EmbeddingModel embeddingModel,
			CardRepository cardRepository,
			@Value("${com.pelia.mtgfinder.embedding-batch-size:32}") int batchSize,
			@Value("${spring.ai.vectorstore.pgvector.dimensions}") int embeddingDimensions) {
		if (batchSize <= 0) {
			throw new IllegalArgumentException("Embedding batch size must be greater than zero");
		}
		if (embeddingDimensions <= 0) {
			throw new IllegalArgumentException("Embedding dimensions must be greater than zero");
		}

		this.embeddingModel = embeddingModel;
		this.cardRepository = cardRepository;
		this.batchSize = batchSize;
		this.embeddingDimensions = embeddingDimensions;
	}

	public EmbeddingResult embedPendingCards() {
		int embeddedCards = 0;
		while (true) {
			// Fetch one representative for each pending card/language pair.
			List<Card> cards = cardRepository.findPendingEmbeddings(batchSize);
			if (cards.isEmpty()) {
				break;
			}

			List<String> contexts = cards.stream()
					.map(this::contextText)
					.toList();

			// Generate embeddings for the batch of cards
			List<float[]> embeddings = embeddingModel.embed(contexts);

			// Validate the embeddings returned by the model
			validateEmbeddings(cards.size(), embeddings);

			// Associate the embeddings with their corresponding cards
			for (int index = 0; index < cards.size(); index++) {
				cards.get(index).setEmbedding(embeddings.get(index));
			}

			cardRepository.saveAllAndFlush(cards);
			embeddedCards += cards.size();
		}

		return new EmbeddingResult(embeddedCards);
	}

	private String contextText(Card card) {
		String contextText = card.getContextText();
		if (contextText == null || contextText.isBlank()) {
			throw new IllegalStateException(
					"Card %s from import %s does not have context text"
							.formatted(card.getId(), card.getImportId()));
		}
		return contextText;
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
}
