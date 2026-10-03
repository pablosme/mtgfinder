package com.pelia.mtgfinder.dto;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallCard(
		String object,
		UUID id,
		UUID oracleId,
		String lang,
		String layout,
		URI printsSearchUri,
		URI rulingsUri,
		URI scryfallUri,
		URI uri,
		Integer arenaId,
		Integer mtgoId,
		Integer mtgoFoilId,
		List<Integer> multiverseIds,
		Integer tcgplayerId,
		Integer tcgplayerEtchedId,
		Integer cardmarketId,
		List<RelatedCard> allParts,
		Map<String, String> legalities,
		List<CardFace> cardFaces,
		String name,
		String typeLine,
		String manaCost,
		BigDecimal cmc,
		String oracleText,
		List<String> colors,
		List<String> colorIdentity,
		List<String> colorIndicator,
		List<String> keywords,
		List<String> producedMana,
		Integer edhrecRank,
		Integer pennyRank,
		Boolean reserved,
		Boolean gameChanger,
		String resourceId,
		OffsetDateTime imageUpdatedAt,
		String power,
		String toughness,
		String loyalty,
		String defense,
		String handModifier,
		String lifeModifier,
		Boolean foil,
		Boolean nonfoil,
		String artist,
		List<UUID> artistIds,
		List<Integer> attractionLights,
		Boolean booster,
		String borderColor,
		UUID cardBackId,
		String collectorNumber,
		Boolean contentWarning,
		Boolean digital,
		List<String> finishes,
		String flavorName,
		String flavorText,
		List<String> frameEffects,
		String frame,
		Boolean fullArt,
		List<String> games,
		Boolean highresImage,
		UUID illustrationId,
		String imageStatus,
		Map<String, URI> imageUris,
		Boolean oversized,
		Prices prices,
		Boolean promo,
		List<String> promoTypes,
		Map<String, URI> purchaseUris,
		String rarity,
		Map<String, URI> relatedUris,
		LocalDate releasedAt,
		Boolean reprint,
		URI scryfallSetUri,
		String setName,
		URI setSearchUri,
		String setType,
		URI setUri,
		String set,
		UUID setId,
		Boolean storySpotlight,
		Boolean textless,
		String securityStamp,
		Preview preview,
		Boolean variation,
		UUID variationOf,
		String watermark,
		String printedName,
		String printedText,
		String printedTypeLine) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record CardFace(
			String object,
			String artist,
			UUID artistId,
			BigDecimal cmc,
			List<String> colorIndicator,
			List<String> colors,
			String defense,
			String flavorName,
			String flavorText,
			UUID illustrationId,
			Map<String, URI> imageUris,
			String layout,
			String loyalty,
			String manaCost,
			String name,
			UUID oracleId,
			String oracleText,
			String power,
			String printedName,
			String printedText,
			String printedTypeLine,
			String toughness,
			String typeLine,
			String watermark) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record RelatedCard(
			String object,
			UUID id,
			String component,
			String name,
			String typeLine,
			URI uri) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record Prices(
			String usd,
			String usdFoil,
			String usdEtched,
			String eur,
			String eurFoil,
			String eurEtched,
			String tix) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record Preview(
			LocalDate previewedAt,
			String sourceUri,
			String source) {
	}
}
