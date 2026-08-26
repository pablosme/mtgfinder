package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.model.ImportLog;
import com.pelia.mtgfinder.repository.ImportLogRepository;

@Service
public class ImportLogService {

    private final ImportLogRepository repository;

    public ImportLogService(ImportLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportLog log(UUID importId, String message) {
        ImportLog importLog = new ImportLog();
        importLog.setImportId(importId);
        importLog.setMessage(message);
        return repository.save(importLog);
    }

    @Transactional(readOnly = true)
    public List<ImportLog> findByImportId(UUID importId) {
        return repository.findByImportIdOrderByIdAsc(importId);
    }
}
