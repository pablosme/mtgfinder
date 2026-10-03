package com.pelia.mtgfinder.repository;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.pelia.mtgfinder.dto.CardFaceImportRow;
import com.pelia.mtgfinder.dto.CardOracleImportRow;
import com.pelia.mtgfinder.dto.CardPrintImportRow;

import tools.jackson.databind.ObjectMapper;

@Component
public class CardImportCopyWriter {

    private final ObjectMapper objectMapper;

    public CardImportCopyWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Values follow CardImportColumns.ORACLE, preceded by any staging identifiers.
    public String writeOracle(CardOracleImportRow card) {
        PostgresCopyRow row = new PostgresCopyRow();
        row.append(card.importId(), UUID::toString);
        row.append(card.sourceCardPrintId(), UUID::toString);
        row.append(card.id(), UUID::toString);
        row.append(card.oracleId(), UUID::toString);
        row.append(card.fallbackPrintId(), UUID::toString);
        row.append(card.name());
        row.append(card.layout());
        row.append(card.manaCost());
        row.append(card.cmc(), BigDecimal::toString);
        row.append(card.typeLine());
        row.append(card.oracleText());
        row.append(card.power());
        row.append(card.toughness());
        row.append(card.loyalty());
        row.append(card.defense());
        row.append(card.handModifier());
        row.append(card.lifeModifier());
        row.appendArray(card.colors(), value -> value);
        row.appendArray(card.colorIdentity(), value -> value);
        row.appendArray(card.colorIndicator(), value -> value);
        row.appendArray(card.keywords(), value -> value);
        row.appendArray(card.producedMana(), value -> value);
        row.append(card.reserved(), value -> value.toString());
        row.append(card.gameChanger(), value -> value.toString());
        row.append(card.edhrecRank(), value -> value.toString());
        row.append(card.pennyRank(), value -> value.toString());
        row.append(card.printsSearchUri(), URI::toString);
        row.append(card.legalities() == null ? null : objectMapper.writeValueAsString(card.legalities()));
        return row.finish();
    }

    // Values follow CardImportColumns.PRINT, preceded by any staging identifiers.
    public String writePrint(CardPrintImportRow card) {
        PostgresCopyRow row = new PostgresCopyRow();
        row.append(card.id(), UUID::toString);
        row.append(card.cardIdentityId(), UUID::toString);
        row.append(card.importId(), UUID::toString);
        row.append(card.metadataHash());
        row.append(card.contextText());
        row.append(card.lang());
        row.append(card.releasedAt(), LocalDate::toString);
        row.appendArray(card.multiverseIds(), value -> value.toString());
        row.append(card.mtgoId(), value -> value.toString());
        row.append(card.mtgoFoilId(), value -> value.toString());
        row.append(card.arenaId(), value -> value.toString());
        row.append(card.tcgplayerId(), value -> value.toString());
        row.append(card.tcgplayerEtchedId(), value -> value.toString());
        row.append(card.cardmarketId(), value -> value.toString());
        row.append(card.resourceId());
        row.append(card.uri(), URI::toString);
        row.append(card.scryfallUri(), URI::toString);
        row.append(card.rulingsUri(), URI::toString);
        row.append(card.setId(), UUID::toString);
        row.append(card.setCode());
        row.append(card.setName());
        row.append(card.setType());
        row.append(card.setUri(), URI::toString);
        row.append(card.setSearchUri(), URI::toString);
        row.append(card.scryfallSetUri(), URI::toString);
        row.append(card.collectorNumber());
        row.append(card.rarity());
        row.append(card.printedName());
        row.append(card.printedTypeLine());
        row.append(card.printedText());
        row.append(card.flavorName());
        row.append(card.flavorText());
        row.append(card.artist());
        row.appendArray(card.artistIds(), UUID::toString);
        row.append(card.illustrationId(), UUID::toString);
        row.append(card.watermark());
        row.append(card.cardBackId(), UUID::toString);
        row.append(card.imageUris() == null ? null : objectMapper.writeValueAsString(card.imageUris()));
        row.append(card.highresImage(), value -> value.toString());
        row.append(card.imageStatus());
        row.append(card.imageUpdatedAt(), OffsetDateTime::toString);
        row.appendArray(card.games(), value -> value);
        row.appendArray(card.finishes(), value -> value);
        row.append(card.foil(), value -> value.toString());
        row.append(card.nonfoil(), value -> value.toString());
        row.append(card.oversized(), value -> value.toString());
        row.append(card.promo(), value -> value.toString());
        row.appendArray(card.promoTypes(), value -> value);
        row.append(card.reprint(), value -> value.toString());
        row.append(card.variation(), value -> value.toString());
        row.append(card.variationOf(), UUID::toString);
        row.append(card.digital(), value -> value.toString());
        row.append(card.borderColor());
        row.append(card.frame());
        row.appendArray(card.frameEffects(), value -> value);
        row.append(card.securityStamp());
        row.append(card.fullArt(), value -> value.toString());
        row.append(card.textless(), value -> value.toString());
        row.append(card.booster(), value -> value.toString());
        row.append(card.storySpotlight(), value -> value.toString());
        row.append(card.contentWarning(), value -> value.toString());
        row.appendArray(card.attractionLights(), value -> value.toString());
        row.append(card.allParts() == null ? null : objectMapper.writeValueAsString(card.allParts()));
        row.append(card.prices() == null ? null : objectMapper.writeValueAsString(card.prices()));
        row.append(card.relatedUris() == null ? null : objectMapper.writeValueAsString(card.relatedUris()));
        row.append(card.purchaseUris() == null ? null : objectMapper.writeValueAsString(card.purchaseUris()));
        row.append(card.preview() == null ? null : objectMapper.writeValueAsString(card.preview()));
        return row.finish();
    }

    // Values follow CardImportColumns.FACE, preceded by any staging identifiers.
    public String writeFace(CardFaceImportRow card) {
        PostgresCopyRow row = new PostgresCopyRow();
        row.append(card.importId(), UUID::toString);
        row.append(card.cardPrintId(), UUID::toString);
        row.append(Integer.toString(card.faceIndex()));
        row.append(card.name());
        row.append(card.manaCost());
        row.append(card.cmc(), BigDecimal::toString);
        row.append(card.typeLine());
        row.append(card.oracleText());
        row.append(card.power());
        row.append(card.toughness());
        row.append(card.loyalty());
        row.append(card.defense());
        row.appendArray(card.colors(), value -> value);
        row.appendArray(card.colorIndicator(), value -> value);
        row.append(card.oracleId(), UUID::toString);
        row.append(card.layout());
        row.append(card.printedName());
        row.append(card.printedTypeLine());
        row.append(card.printedText());
        row.append(card.flavorName());
        row.append(card.flavorText());
        row.append(card.artist());
        row.append(card.artistId(), UUID::toString);
        row.append(card.illustrationId(), UUID::toString);
        row.append(card.watermark());
        row.append(card.imageUris() == null ? null : objectMapper.writeValueAsString(card.imageUris()));
        return row.finish();
    }
}
