package com.pelia.mtgfinder.dto;

import java.util.List;

public record StagedCardRows(
        CardOracleImportRow oracle,
        CardPrintImportRow print,
        List<CardFaceImportRow> faces) {
}
