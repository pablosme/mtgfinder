package com.pelia.mtgfinder.dto;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pelia.mtgfinder.dto.ScryfallCard.Preview;
import com.pelia.mtgfinder.dto.ScryfallCard.Prices;
import com.pelia.mtgfinder.dto.ScryfallCard.RelatedCard;

public record CardPrintImportRow(
        UUID id,
        UUID cardIdentityId,
        UUID importId,
        String metadataHash,
        String contextText,
        String lang,
        LocalDate releasedAt,
        List<Integer> multiverseIds,
        Integer mtgoId,
        Integer mtgoFoilId,
        Integer arenaId,
        Integer tcgplayerId,
        Integer tcgplayerEtchedId,
        Integer cardmarketId,
        String resourceId,
        URI uri,
        URI scryfallUri,
        URI rulingsUri,
        UUID setId,
        String setCode,
        String setName,
        String setType,
        URI setUri,
        URI setSearchUri,
        URI scryfallSetUri,
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
        Map<String, URI> imageUris,
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
        List<Integer> attractionLights,
        List<RelatedCard> allParts,
        Prices prices,
        Map<String, URI> relatedUris,
        Map<String, URI> purchaseUris,
        Preview preview) {
}
