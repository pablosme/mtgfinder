package com.pelia.mtgfinder.service;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pelia.mtgfinder.dto.EmbeddingResult;
import com.pelia.mtgfinder.dto.PendingCardEmbedding;
import com.pelia.mtgfinder.repository.CardEmbeddingRepository;

@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;
    private final CardEmbeddingRepository repository;
    private final int batchSize;
    private final int embeddingDimensions;

    public EmbeddingService(EmbeddingModel embeddingModel, CardEmbeddingRepository repository,
            @Value("${com.pelia.mtgfinder.embedding-batch-size:32}") int batchSize,
            @Value("${spring.ai.vectorstore.pgvector.dimensions}") int embeddingDimensions) {
        if (batchSize <= 0) throw new IllegalArgumentException("Embedding batch size must be greater than zero");
        if (embeddingDimensions != 1024) {
            throw new IllegalArgumentException("Embedding dimensions must match the database schema: 1024");
        }
        this.embeddingModel = embeddingModel;
        this.repository = repository;
        this.batchSize = batchSize;
        this.embeddingDimensions = embeddingDimensions;
    }

    public EmbeddingResult embedPendingCards() {
        int embeddedCards = 0;
        while (true) {
            List<PendingCardEmbedding> pending = repository.findPending(batchSize);
            if (pending.isEmpty()) break;
            for (PendingCardEmbedding source : pending) {
                if (source.contextText() == null || source.contextText().isBlank()) {
                    throw new IllegalStateException("Print " + source.cardPrintId() + " has no embedding context");
                }
            }
            List<float[]> vectors = embeddingModel.embed(pending.stream()
                    .map(PendingCardEmbedding::contextText).toList());
            validateEmbeddings(pending.size(), vectors);
            embeddedCards += repository.saveBatch(pending, vectors);
        }
        return new EmbeddingResult(embeddedCards);
    }

    private void validateEmbeddings(int expectedCount, List<float[]> embeddings) {
        if (embeddings == null || embeddings.size() != expectedCount) {
            throw new IllegalStateException("Embedding model returned an unexpected number of vectors");
        }
        for (float[] embedding : embeddings) {
            if (embedding == null || embedding.length != embeddingDimensions) {
                throw new IllegalStateException("Embedding model must return " + embeddingDimensions + " dimensions");
            }
            boolean nonzero = false;
            for (float value : embedding) {
                if (!Float.isFinite(value)) throw new IllegalStateException("Embedding contains a non-finite value");
                nonzero |= value != 0;
            }
            if (!nonzero) throw new IllegalStateException("A zero vector cannot be used for cosine search");
        }
    }
}
