package com.pelia.mtgfinder.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pelia.mtgfinder.dto.EmbeddingResult;
import com.pelia.mtgfinder.dto.ScryfallImportResponse;
import com.pelia.mtgfinder.service.EmbeddingService;
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
    public EmbeddingResult embed() {
        System.out.println("Embedding all cards without an embedding");
        EmbeddingResult result = embeddingService.embedPendingCards();
        System.out.println("Embedding completed. Result: " + result);
        return result;
    }

}
