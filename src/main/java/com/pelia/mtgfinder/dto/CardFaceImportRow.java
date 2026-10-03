package com.pelia.mtgfinder.dto;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CardFaceImportRow(
        UUID importId,
        UUID cardPrintId,
        int faceIndex,
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
        Map<String, URI> imageUris) {
}
