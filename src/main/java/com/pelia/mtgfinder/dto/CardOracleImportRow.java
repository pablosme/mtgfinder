package com.pelia.mtgfinder.dto;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CardOracleImportRow(
        UUID importId,
        UUID sourceCardPrintId,
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
        Boolean reserved,
        Boolean gameChanger,
        Integer edhrecRank,
        Integer pennyRank,
        URI printsSearchUri,
        Map<String, String> legalities) {
}
