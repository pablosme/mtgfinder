package com.pelia.mtgfinder.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pelia.mtgfinder.dto.ScryfallImportResponse;
import com.pelia.mtgfinder.service.EmbeddingService;
import com.pelia.mtgfinder.service.EmbeddingService.EmbeddingResult;
import com.pelia.mtgfinder.service.ScryfallImportService;

@RestController
@RequestMapping("/import")
public class ImportController {

    private final ScryfallImportService scryfallImportService;
    private final EmbeddingService embeddingService;

    public ImportController(ScryfallImportService scryfallImportService, EmbeddingService embeddingService) {
        this.scryfallImportService = scryfallImportService;
        this.embeddingService = embeddingService;
    }

    @PostMapping("/bulk-data")
    public ScryfallImportResponse importBulkData(@RequestParam String bulkType) {
        System.out.println("Importing bulk data of type: " + bulkType);
        return scryfallImportService.importCards(bulkType);
    }

    @PostMapping("/embed")
    public EmbeddingResult embed(@RequestParam UUID importId) {
        System.out.println("Embedding data for import ID: " + importId);
        EmbeddingResult result = embeddingService.embedImport(importId);
        System.out.println("Embedding completed for import ID: " + importId + ". Result: " + result);
        return result;
    }

}
