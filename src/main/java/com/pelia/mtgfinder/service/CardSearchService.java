package com.pelia.mtgfinder.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.dto.CardSearchResponse;
import com.pelia.mtgfinder.dto.CardSearchFilters;
import com.pelia.mtgfinder.model.CardOracle;
import com.pelia.mtgfinder.repository.CardOracleRepository;
import com.pelia.mtgfinder.repository.OracleSearchMatch;

@Service
@Transactional(readOnly = true)
public class CardSearchService {

    private final CardOracleRepository cardOracleRepository;
    private final CardResponseMapper responseMapper;
    private final EmbeddingModel embeddingModel;
    private final int vectorSearchLimit;
    private final double fuzzySearchThreshold;

    public CardSearchService(
            CardOracleRepository cardOracleRepository,
            CardResponseMapper responseMapper,
            EmbeddingModel embeddingModel,
            @Value("${com.pelia.mtgfinder.vector-search-limit:10}") int vectorSearchLimit,
            @Value("${com.pelia.mtgfinder.fuzzy-search-threshold:0.35}") double fuzzySearchThreshold) {
        if (!Double.isFinite(fuzzySearchThreshold) || fuzzySearchThreshold < 0.3 || fuzzySearchThreshold > 1) {
            throw new IllegalArgumentException("Fuzzy search threshold must be between 0.3 and 1");
        }
        if (vectorSearchLimit < 1) {
            throw new IllegalArgumentException("Vector search limit must be positive");
        }

        this.cardOracleRepository = cardOracleRepository;
        this.responseMapper = responseMapper;
        this.embeddingModel = embeddingModel;
        this.vectorSearchLimit = vectorSearchLimit;
        this.fuzzySearchThreshold = fuzzySearchThreshold;
    }

    public CardSearchResponse searchByName(String name, CardSearchFilters filters) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        String query = name.strip();
        List<CardSearchResponse.OracleCard> data = cardOracleRepository.searchByName(query, filters).stream()
                .map(oracle -> responseMapper.toOracleCard(oracle, null, null))
                .toList();
        return new CardSearchResponse(data.size(), query, data);
    }

    public CardSearchResponse searchByFuzzyName(String name, CardSearchFilters filters) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        String query = name.strip();
        List<OracleSearchMatch> matches = cardOracleRepository.searchByFuzzyName(query, fuzzySearchThreshold, filters);
        return toSearchResponse(query, matches);
    }

    public CardSearchResponse searchByPrompt(String prompt, CardSearchFilters filters) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }

        String query = prompt.strip();
        float[] promptEmbedding = embeddingModel.embed(query);
        List<OracleSearchMatch> matches = cardOracleRepository.searchByEmbedding(
                Arrays.toString(promptEmbedding), vectorSearchLimit, filters);
        return toSearchResponse(query, matches);
    }

    private CardSearchResponse toSearchResponse(String query, List<OracleSearchMatch> matches) {
        if (matches.isEmpty()) {
            return new CardSearchResponse(0, query, List.of());
        }

        List<UUID> matchingIds = matches.stream().map(OracleSearchMatch::getCardIdentityId).toList();
        Map<UUID, CardOracle> oraclesById = cardOracleRepository.findAllById(matchingIds).stream()
                .collect(Collectors.toMap(CardOracle::getId, oracle -> oracle));
        List<CardSearchResponse.OracleCard> data = matches.stream()
                .filter(match -> oraclesById.containsKey(match.getCardIdentityId()))
                .map(match -> responseMapper.toOracleCard(
                        oraclesById.get(match.getCardIdentityId()), match.getSimilarityScore(), null))
                .toList();
        return new CardSearchResponse(data.size(), query, data);
    }
}
