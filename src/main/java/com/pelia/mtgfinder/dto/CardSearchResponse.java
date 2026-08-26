package com.pelia.mtgfinder.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CardSearchResponse(
        int totalResults,
        String query,
        List<OracleCard> data) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record OracleCard(
            UUID oracleId,
            String name,
            String layout,
            String manaCost,
            BigDecimal cmc,
            String typeLine,
            String oracleText,
            List<String> colors,
            List<String> colorIdentity,
            List<String> keywords,
            JsonNode legalities,
            Double similarityScore,
            List<CardFace> faces,
            List<CardPrint> prints) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CardFace(
            short faceIndex,
            String name,
            String manaCost,
            String typeLine,
            String oracleText,
            List<String> colors,
            String power,
            String toughness,
            String loyalty) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CardPrint(
            UUID id,
            String setCode,
            String setName,
            String collectorNumber,
            String rarity,
            String flavorText,
            String artist,
            LocalDate releasedAt,
            Object imageUris,
            JsonNode prices,
            JsonNode relatedUris) {
    }
}
