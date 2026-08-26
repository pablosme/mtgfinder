package com.pelia.mtgfinder.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.pelia.mtgfinder.model.BulkImport;
import com.pelia.mtgfinder.model.ImportStatus;

public record ScryfallImportResponse(
        UUID importId,
        String scryfallBulkType,
        OffsetDateTime scryfallUpdatedAt,
        ImportStatus status,
        Integer totalCards,
        OffsetDateTime createdAt,
        OffsetDateTime activatedAt,
        List<ImportLogResponse> logs) {

    public static ScryfallImportResponse from(BulkImport bulkImport, List<ImportLogResponse> logs) {
        return new ScryfallImportResponse(
                bulkImport.getId(),
                bulkImport.getScryfallBulkType(),
                bulkImport.getScryfallUpdatedAt(),
                bulkImport.getStatus(),
                bulkImport.getTotalCards(),
                bulkImport.getCreatedAt(),
                bulkImport.getActivatedAt(),
                logs);
    }
}
