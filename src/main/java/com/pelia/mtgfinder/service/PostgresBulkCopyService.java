package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.dto.ImportCardCounts;
import com.pelia.mtgfinder.dto.ScryfallCard;
import com.pelia.mtgfinder.dto.StagedCardRows;
import com.pelia.mtgfinder.repository.CardImportRepository;

import tools.jackson.databind.ObjectMapper;

@Service
// Each batch commits independently; promotion commits all live changes together.
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class PostgresBulkCopyService {

    private final CardImportRepository repository;
    private final ObjectMapper objectMapper;
    private final ScryfallCardMapper cardMapper;
    private final CardContextService contextService;

    public PostgresBulkCopyService(CardImportRepository repository, ObjectMapper objectMapper,
            ScryfallCardMapper cardMapper, CardContextService contextService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.cardMapper = cardMapper;
        this.contextService = contextService;
    }

    public void insertBatchCopy(UUID importId, List<ScryfallCard> cards) {
        Objects.requireNonNull(importId, "importId must not be null");
        List<StagedCardRows> rows = cards.stream()
                .map(card -> cardMapper.toRows(importId, card))
                .toList();
        repository.stageBatch(rows);
    }

    public void finalizeImport(UUID importId, int totalCards) {
        Objects.requireNonNull(importId, "importId must not be null");
        if (totalCards < 0) {
            throw new IllegalArgumentException("totalCards must not be negative");
        }

        repository.lockProcessingImport(importId);
        ImportCardCounts counts = repository.countStagedCards(importId);
        if (counts.total() != totalCards) {
            throw new IllegalStateException("Staged print count does not match import count");
        }

        repository.mergeOracles(importId, CardContextService.ORACLE_CONTEXT_FIELDS);
        repository.mergePrintsAndFaces(importId);

        // Shared gameplay changes also affect retained prints outside this import.
        repository.refreshContexts(importId,
                json -> contextService.build(objectMapper.readValue(json, ScryfallCard.class)));

        repository.activateImport(importId, counts);
        repository.deleteStagedRows(importId);
        repository.deleteUnusedOracles();
    }

    public void discardStagedImport(UUID importId) {
        Objects.requireNonNull(importId, "importId must not be null");
        repository.deleteStagedRows(importId);
    }
}
