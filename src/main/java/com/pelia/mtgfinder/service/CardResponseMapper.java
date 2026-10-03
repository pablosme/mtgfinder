package com.pelia.mtgfinder.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.pelia.mtgfinder.dto.CardSearchResponse;
import com.pelia.mtgfinder.model.Card;
import com.pelia.mtgfinder.model.CardFace;
import com.pelia.mtgfinder.model.CardOracle;

@Component
public class CardResponseMapper {

    public CardSearchResponse.OracleCard toOracleCard(
            CardOracle oracle, Double similarityScore, List<CardSearchResponse.CardPrint> prints) {
        return new CardSearchResponse.OracleCard(
                oracle.getId(),
                oracle.getOracleId(),
                oracle.getFallbackPrintId(),
                oracle.getName(),
                oracle.getLayout(),
                oracle.getManaCost(),
                oracle.getCmc(),
                oracle.getTypeLine(),
                oracle.getOracleText(),
                oracle.getPower(),
                oracle.getToughness(),
                oracle.getLoyalty(),
                oracle.getDefense(),
                oracle.getHandModifier(),
                oracle.getLifeModifier(),
                toList(oracle.getColors()),
                toList(oracle.getColorIdentity()),
                toList(oracle.getColorIndicator()),
                toList(oracle.getKeywords()),
                toList(oracle.getProducedMana()),
                oracle.getReserved(),
                oracle.getGameChanger(),
                oracle.getEdhrecRank(),
                oracle.getPennyRank(),
                oracle.getPrintsSearchUri(),
                oracle.getLegalities(),
                similarityScore,
                prints);
    }

    public CardSearchResponse.CardPrint toCardPrint(Card card, List<CardFace> faces) {
        return new CardSearchResponse.CardPrint(
                card.getId(),
                card.getCardIdentityId(),
                card.getImportId(),
                card.getMetadataHash(),
                card.getContextText(),
                card.getCreatedAt(),
                card.getLang(),
                card.getReleasedAt(),
                toList(card.getMultiverseIds()),
                card.getMtgoId(),
                card.getMtgoFoilId(),
                card.getArenaId(),
                card.getTcgplayerId(),
                card.getTcgplayerEtchedId(),
                card.getCardmarketId(),
                card.getResourceId(),
                card.getUri(),
                card.getScryfallUri(),
                card.getRulingsUri(),
                card.getSetId(),
                card.getSetCode(),
                card.getSetName(),
                card.getSetType(),
                card.getSetUri(),
                card.getSetSearchUri(),
                card.getScryfallSetUri(),
                card.getCollectorNumber(),
                card.getRarity() == null ? null : card.getRarity().name().toLowerCase(Locale.ROOT),
                card.getPrintedName(),
                card.getPrintedTypeLine(),
                card.getPrintedText(),
                card.getFlavorName(),
                card.getFlavorText(),
                card.getArtist(),
                toList(card.getArtistIds()),
                card.getIllustrationId(),
                card.getWatermark(),
                card.getCardBackId(),
                card.getImageUris(),
                card.getHighresImage(),
                card.getImageStatus(),
                card.getImageUpdatedAt(),
                toList(card.getGames()),
                toList(card.getFinishes()),
                card.getFoil(),
                card.getNonfoil(),
                card.getOversized(),
                card.getPromo(),
                toList(card.getPromoTypes()),
                card.getReprint(),
                card.getVariation(),
                card.getVariationOf(),
                card.getDigital(),
                card.getBorderColor(),
                card.getFrame(),
                toList(card.getFrameEffects()),
                card.getSecurityStamp(),
                card.getFullArt(),
                card.getTextless(),
                card.getBooster(),
                card.getStorySpotlight(),
                card.getContentWarning(),
                toList(card.getAttractionLights()),
                card.getAllParts(),
                card.getPrices(),
                card.getRelatedUris(),
                card.getPurchaseUris(),
                card.getPreview(),
                faces.stream().map(this::toCardFace).toList());
    }

    private CardSearchResponse.CardFace toCardFace(CardFace face) {
        return new CardSearchResponse.CardFace(
                face.getCardPrintId(),
                face.getFaceIndex(),
                face.getName(),
                face.getManaCost(),
                face.getCmc(),
                face.getTypeLine(),
                face.getOracleText(),
                face.getPower(),
                face.getToughness(),
                face.getLoyalty(),
                face.getDefense(),
                toList(face.getColors()),
                toList(face.getColorIndicator()),
                face.getOracleId(),
                face.getLayout(),
                face.getPrintedName(),
                face.getPrintedTypeLine(),
                face.getPrintedText(),
                face.getFlavorName(),
                face.getFlavorText(),
                face.getArtist(),
                face.getArtistId(),
                face.getIllustrationId(),
                face.getWatermark(),
                face.getImageUris());
    }

    private <T> List<T> toList(T[] values) {
        return values == null ? null : Arrays.asList(values);
    }
}
