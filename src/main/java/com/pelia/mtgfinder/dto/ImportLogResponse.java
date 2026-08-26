package com.pelia.mtgfinder.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.pelia.mtgfinder.model.ImportLog;

public record ImportLogResponse(
        Long id,
        UUID importId,
        OffsetDateTime createdAt,
        String message) {

    public static ImportLogResponse from(ImportLog importLog) {
        return new ImportLogResponse(
                importLog.getId(),
                importLog.getImportId(),
                importLog.getCreatedAt(),
                importLog.getMessage());
    }
}
