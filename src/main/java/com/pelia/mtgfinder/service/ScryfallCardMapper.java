package com.pelia.mtgfinder.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.pelia.mtgfinder.dto.CardFaceImportRow;
import com.pelia.mtgfinder.dto.CardOracleImportRow;
import com.pelia.mtgfinder.dto.CardPrintImportRow;
import com.pelia.mtgfinder.dto.ScryfallCard;
import com.pelia.mtgfinder.dto.StagedCardRows;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;

@Component
public class ScryfallCardMapper {

    private final ObjectWriter metadataWriter;
    private final CardContextService contextService;

    public ScryfallCardMapper(ObjectMapper objectMapper, CardContextService contextService) {
        // Stable ordering includes nested records and maps, independent of JSON input key order.
        this.metadataWriter = objectMapper.rebuild()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build()
                .writerFor(ScryfallCard.class);
        this.contextService = contextService;
    }

    public StagedCardRows toRows(UUID importId, ScryfallCard card) {
        Objects.requireNonNull(importId, "Import id is required");
        Objects.requireNonNull(card.id(), "Scryfall print id is required");
        Objects.requireNonNull(card.lang(), "Scryfall language is required");
        String sourceKey = card.oracleId() == null ? "print:" + card.id() : "oracle:" + card.oracleId();
        UUID identityId = UUID.nameUUIDFromBytes(sourceKey.getBytes(StandardCharsets.UTF_8));
        String metadataHash;
        try {
            metadataHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(metadataWriter.writeValueAsBytes(card)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }

        CardOracleImportRow oracle = new CardOracleImportRow(
                importId,
                card.id(),
                identityId,
                card.oracleId(),
                card.oracleId() == null ? card.id() : null,
                card.name(),
                card.layout(),
                card.manaCost(),
                card.cmc(),
                card.typeLine(),
                card.oracleText(),
                card.power(),
                card.toughness(),
                card.loyalty(),
                card.defense(),
                card.handModifier(),
                card.lifeModifier(),
                card.colors(),
                card.colorIdentity() == null ? List.of() : card.colorIdentity(),
                card.colorIndicator(),
                card.keywords() == null ? List.of() : card.keywords(),
                card.producedMana(),
                Boolean.TRUE.equals(card.reserved()),
                card.gameChanger(),
                card.edhrecRank(),
                card.pennyRank(),
                card.printsSearchUri(),
                card.legalities() == null ? Map.of() : card.legalities());

        CardPrintImportRow print = new CardPrintImportRow(
                card.id(),
                identityId,
                importId,
                metadataHash,
                contextService.build(card),
                card.lang(),
                card.releasedAt(),
                card.multiverseIds(),
                card.mtgoId(),
                card.mtgoFoilId(),
                card.arenaId(),
                card.tcgplayerId(),
                card.tcgplayerEtchedId(),
                card.cardmarketId(),
                card.resourceId(),
                card.uri(),
                card.scryfallUri(),
                card.rulingsUri(),
                card.setId(),
                card.set(),
                card.setName(),
                card.setType(),
                card.setUri(),
                card.setSearchUri(),
                card.scryfallSetUri(),
                card.collectorNumber(),
                card.rarity(),
                card.printedName(),
                card.printedTypeLine(),
                card.printedText(),
                card.flavorName(),
                card.flavorText(),
                card.artist(),
                card.artistIds(),
                card.illustrationId(),
                card.watermark(),
                card.cardBackId(),
                card.imageUris(),
                card.highresImage(),
                card.imageStatus(),
                card.imageUpdatedAt(),
                card.games(),
                card.finishes(),
                card.foil(),
                card.nonfoil(),
                card.oversized(),
                card.promo(),
                card.promoTypes(),
                card.reprint(),
                card.variation(),
                card.variationOf(),
                card.digital(),
                card.borderColor(),
                card.frame(),
                card.frameEffects(),
                card.securityStamp(),
                card.fullArt(),
                card.textless(),
                card.booster(),
                card.storySpotlight(),
                card.contentWarning(),
                card.attractionLights(),
                card.allParts(),
                card.prices(),
                card.relatedUris(),
                card.purchaseUris(),
                card.preview());

        List<CardFaceImportRow> faces = new ArrayList<>();
        if (card.cardFaces() != null) {
            for (int index = 0; index < card.cardFaces().size(); index++) {
                ScryfallCard.CardFace face = card.cardFaces().get(index);
                faces.add(new CardFaceImportRow(
                        importId,
                        card.id(),
                        index,
                        face.name(),
                        face.manaCost(),
                        face.cmc(),
                        face.typeLine(),
                        face.oracleText(),
                        face.power(),
                        face.toughness(),
                        face.loyalty(),
                        face.defense(),
                        face.colors(),
                        face.colorIndicator(),
                        face.oracleId(),
                        face.layout(),
                        face.printedName(),
                        face.printedTypeLine(),
                        face.printedText(),
                        face.flavorName(),
                        face.flavorText(),
                        face.artist(),
                        face.artistId(),
                        face.illustrationId(),
                        face.watermark(),
                        face.imageUris()));
            }
        }
        return new StagedCardRows(oracle, print, faces);
    }
}
