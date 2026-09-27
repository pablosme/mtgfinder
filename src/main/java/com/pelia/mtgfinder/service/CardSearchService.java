package com.pelia.mtgfinder.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.dto.CardSearchResponse;
import com.pelia.mtgfinder.model.ActiveCard;
import com.pelia.mtgfinder.model.CardFace;
import com.pelia.mtgfinder.repository.ActiveCardRepository;
import com.pelia.mtgfinder.repository.ActiveCardRepository.FuzzyMatch;
import com.pelia.mtgfinder.repository.ActiveCardRepository.VectorMatch;
import com.pelia.mtgfinder.repository.CardFaceRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class CardSearchService {

    private final ActiveCardRepository activeCardRepository;
    private final CardFaceRepository cardFaceRepository;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;
    private final int vectorSearchLimit;
    private final double fuzzySearchThreshold;

    public CardSearchService(
            ActiveCardRepository activeCardRepository,
            CardFaceRepository cardFaceRepository,
            ObjectMapper objectMapper,
            EmbeddingModel embeddingModel,
            @Value("${com.pelia.mtgfinder.vector-search-limit:10}") int vectorSearchLimit,
            @Value("${com.pelia.mtgfinder.fuzzy-search-threshold:0.35}") double fuzzySearchThreshold) {
        if (fuzzySearchThreshold < 0.3 || fuzzySearchThreshold > 1) {
            throw new IllegalArgumentException("Fuzzy search threshold must be between 0.3 and 1");
        }

        this.activeCardRepository = activeCardRepository;
        this.cardFaceRepository = cardFaceRepository;
        this.objectMapper = objectMapper;
        this.embeddingModel = embeddingModel;
        this.vectorSearchLimit = vectorSearchLimit;
        this.fuzzySearchThreshold = fuzzySearchThreshold;
    }

    public CardSearchResponse searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        String query = name.strip();
        List<ActiveCard> matchingCards = activeCardRepository
                .searchByNameIncludingFacesAndVariations(query);

        return toSearchResponse(query, matchingCards, Map.of());
    }

    public CardSearchResponse searchByFuzzyName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        String query = name.strip();
        List<FuzzyMatch> matches = activeCardRepository.searchByFuzzyName(
                query, fuzzySearchThreshold);

        List<UUID> matchingIds = matches.stream()
                .map(FuzzyMatch::getCardId)
                .toList();
        Map<UUID, ActiveCard> cardsById = activeCardRepository.findAllById(matchingIds).stream()
                .collect(Collectors.toMap(ActiveCard::getId, card -> card));
        List<ActiveCard> matchingCards = matchingIds.stream()
                .map(cardsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<UUID, Double> similarityScores = matches.stream()
                .collect(Collectors.toMap(
                        FuzzyMatch::getCardId,
                        FuzzyMatch::getSimilarityScore));

        return toSearchResponse(query, matchingCards, similarityScores);
    }

    public CardSearchResponse searchByPrompt(String prompt) {
        float[] promptEmbedding = embeddingModel.embed(prompt);
        System.out.println("Prompt embedding: " + Arrays.toString(promptEmbedding));
        List<VectorMatch> matches = activeCardRepository.searchByEmbedding(
                Arrays.toString(promptEmbedding), vectorSearchLimit);

        List<UUID> matchingIds = matches.stream()
                .map(VectorMatch::getCardId)
                .toList();
        Map<UUID, ActiveCard> cardsById = activeCardRepository.findAllById(matchingIds).stream()
                .collect(Collectors.toMap(ActiveCard::getId, card -> card));
        List<ActiveCard> matchingCards = matchingIds.stream()
                .map(cardsById::get)
                .toList();
        Map<UUID, Double> similarityScores = matches.stream()
                .collect(Collectors.toMap(
                        VectorMatch::getCardId,
                        VectorMatch::getSimilarityScore));

        return toSearchResponse(prompt, matchingCards, similarityScores);
    }

    private CardSearchResponse toSearchResponse(
            String query,
            List<ActiveCard> matchingCards,
            Map<UUID, Double> similarityScores) {

        if (matchingCards.isEmpty()) {
            return new CardSearchResponse(0, query, List.of());
        }

        Set<UUID> matchingCardIds = matchingCards.stream()
                .map(ActiveCard::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, List<CardFace>> facesByCard = cardFaceRepository
                .findForActiveCardsByCardIds(matchingCardIds)
                .stream()
                .collect(Collectors.groupingBy(
                        CardFace::getCardId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<String, List<ActiveCard>> cardsByIdentity = matchingCards.stream()
                .collect(Collectors.groupingBy(
                        card -> identityKey(card, facesByCard),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<CardSearchResponse.OracleCard> data = cardsByIdentity.values().stream()
                .map(cards -> toOracleCard(cards, facesByCard, similarityScores))
                .toList();

        return new CardSearchResponse(data.size(), query, data);
    }

    private String identityKey(ActiveCard card, Map<UUID, List<CardFace>> facesByCard) {
        if (card.getOracleId() != null) {
            return "oracle:" + card.getOracleId();
        }

        String faceOracleIds = facesByCard.getOrDefault(card.getId(), List.of()).stream()
                .map(CardFace::getOracleId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        return faceOracleIds.isEmpty()
                ? "card:" + card.getId()
                : "faces:" + faceOracleIds;
    }

    private CardSearchResponse.OracleCard toOracleCard(
            List<ActiveCard> cards,
            Map<UUID, List<CardFace>> facesByCard,
            Map<UUID, Double> similarityScores) {
        List<ActiveCard> uniquePrints = new ArrayList<>(cards.stream()
                .collect(Collectors.toMap(
                        ActiveCard::getId,
                        card -> card,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .values());

        ActiveCard representative = uniquePrints.getFirst();
        List<CardFace> representativeFaces = facesByCard.getOrDefault(representative.getId(), List.of());

        List<CardSearchResponse.CardFace> faces = representativeFaces.stream()
                .map(this::toCardFace)
                .toList();

        List<CardSearchResponse.CardPrint> prints = uniquePrints.stream()
                .map(card -> toCardPrint(card, facesByCard.getOrDefault(card.getId(), List.of())))
                .toList();

        Double similarityScore = uniquePrints.stream()
                .map(ActiveCard::getId)
                .map(similarityScores::get)
                .filter(java.util.Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);

        return new CardSearchResponse.OracleCard(
                representative.getOracleId(),
                representative.getName(),
                representative.getLayout(),
                manaCost(representative, representativeFaces),
                representative.getCmc(),
                representative.getTypeLine(),
                representative.getOracleText(),
                colors(representative, representativeFaces),
                toList(representative.getColorIdentity()),
                toList(representative.getKeywords()),
                toJsonNode(representative.getLegalities()),
                similarityScore,
                faces,
                prints);
    }

    private CardSearchResponse.CardFace toCardFace(CardFace face) {
        return new CardSearchResponse.CardFace(
                face.getFaceIndex(),
                face.getName(),
                face.getManaCost(),
                face.getTypeLine(),
                face.getOracleText(),
                toList(face.getColors()),
                face.getPower(),
                face.getToughness(),
                face.getLoyalty());
    }

    private CardSearchResponse.CardPrint toCardPrint(ActiveCard card, List<CardFace> faces) {
        String rarity = card.getRarity() == null
                ? null
                : card.getRarity().name().toLowerCase(Locale.ROOT);

        return new CardSearchResponse.CardPrint(
                card.getId(),
                card.getSetCode(),
                card.getSetName(),
                card.getCollectorNumber(),
                rarity,
                card.getFlavorText(),
                card.getArtist(),
                card.getReleasedAt(),
                imageUris(card, faces),
                toJsonNode(card.getPrices()),
                toJsonNode(card.getRelatedUris()));
    }

    private String manaCost(ActiveCard card, List<CardFace> faces) {
        if (card.getManaCost() != null) {
            return card.getManaCost();
        }

        String joinedManaCosts = faces.stream()
                .map(CardFace::getManaCost)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" // "));
        return joinedManaCosts.isEmpty() ? null : joinedManaCosts;
    }

    private List<String> colors(ActiveCard card, List<CardFace> faces) {
        if (card.getColors() != null) {
            return toList(card.getColors());
        }

        Set<String> colors = new LinkedHashSet<>();
        faces.stream()
                .map(CardFace::getColors)
                .filter(values -> values != null)
                .forEach(values -> colors.addAll(Arrays.asList(values)));
        return new ArrayList<>(colors);
    }

    private Object imageUris(ActiveCard card, List<CardFace> faces) {
        if (card.getImageUris() != null) {
            return toJsonNode(card.getImageUris());
        }

        Map<String, Object> imageUris = new LinkedHashMap<>();
        for (CardFace face : faces) {
            if (face.getImageUris() == null) {
                continue;
            }

            String key = switch (face.getFaceIndex()) {
                case 0 -> "front";
                case 1 -> "back";
                default -> "face_" + face.getFaceIndex();
            };
            imageUris.put(key, toJsonNode(face.getImageUris()));
        }
        return imageUris.isEmpty() ? null : imageUris;
    }

    private List<String> toList(String[] values) {
        return values == null ? null : List.copyOf(Arrays.asList(values));
    }

    private JsonNode toJsonNode(JsonNode value) {
        return value == null ? null : objectMapper.readTree(value.toString());
    }

}
