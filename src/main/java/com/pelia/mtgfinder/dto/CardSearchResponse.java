package com.pelia.mtgfinder.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
            UUID id,
            UUID oracleId,
            UUID fallbackPrintId,
            String name,
            String layout,
            String manaCost,
            BigDecimal cmc,
            String typeLine,
            String oracleText,
            String power,
            String toughness,
            String loyalty,
            String defense,
            String handModifier,
            String lifeModifier,
            List<String> colors,
            List<String> colorIdentity,
            List<String> colorIndicator,
            List<String> keywords,
            List<String> producedMana,
            boolean reserved,
            Boolean gameChanger,
            Integer edhrecRank,
            Integer pennyRank,
            String printsSearchUri,
            JsonNode legalities,
            Double similarityScore,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<CardPrint> prints) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CardPrint(
            UUID id,
            UUID cardIdentityId,
            UUID importId,
            String metadataHash,
            String contextText,
            OffsetDateTime createdAt,
            String lang,
            LocalDate releasedAt,
            List<Long> multiverseIds,
            Long mtgoId,
            Long mtgoFoilId,
            Long arenaId,
            Long tcgplayerId,
            Long tcgplayerEtchedId,
            Long cardmarketId,
            String resourceId,
            String uri,
            String scryfallUri,
            String rulingsUri,
            UUID setId,
            String setCode,
            String setName,
            String setType,
            String setUri,
            String setSearchUri,
            String scryfallSetUri,
            String collectorNumber,
            String rarity,
            String printedName,
            String printedTypeLine,
            String printedText,
            String flavorName,
            String flavorText,
            String artist,
            List<UUID> artistIds,
            UUID illustrationId,
            String watermark,
            UUID cardBackId,
            JsonNode imageUris,
            Boolean highresImage,
            String imageStatus,
            OffsetDateTime imageUpdatedAt,
            List<String> games,
            List<String> finishes,
            Boolean foil,
            Boolean nonfoil,
            Boolean oversized,
            Boolean promo,
            List<String> promoTypes,
            Boolean reprint,
            Boolean variation,
            UUID variationOf,
            Boolean digital,
            String borderColor,
            String frame,
            List<String> frameEffects,
            String securityStamp,
            Boolean fullArt,
            Boolean textless,
            Boolean booster,
            Boolean storySpotlight,
            Boolean contentWarning,
            List<Short> attractionLights,
            JsonNode allParts,
            JsonNode prices,
            JsonNode relatedUris,
            JsonNode purchaseUris,
            JsonNode preview,
            List<CardFace> faces) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CardFace(
            UUID cardPrintId,
            short faceIndex,
            String name,
            String manaCost,
            BigDecimal cmc,
            String typeLine,
            String oracleText,
            String power,
            String toughness,
            String loyalty,
            String defense,
            List<String> colors,
            List<String> colorIndicator,
            UUID oracleId,
            String layout,
            String printedName,
            String printedTypeLine,
            String printedText,
            String flavorName,
            String flavorText,
            String artist,
            UUID artistId,
            UUID illustrationId,
            String watermark,
            JsonNode imageUris) {
    }
}
