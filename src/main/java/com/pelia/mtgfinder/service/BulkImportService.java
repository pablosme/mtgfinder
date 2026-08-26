package com.pelia.mtgfinder.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.model.BulkImport;
import com.pelia.mtgfinder.model.ImportStatus;
import com.pelia.mtgfinder.repository.BulkImportRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class BulkImportService {

	private final BulkImportRepository repository;

	public BulkImportService(BulkImportRepository repository) {
		this.repository = repository;
	}

	public List<BulkImport> findAll() {
		return repository.findAll();
	}

	public BulkImport findById(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Bulk import not found: " + id));
	}

	public List<BulkImport> findActive() {
		return repository.findByStatus(ImportStatus.ACTIVE);
	}

	public List<BulkImport> findByStatus(ImportStatus status) {
		return repository.findByStatus(status);
	}

	@Transactional
	public BulkImport save(BulkImport bulkImport) {
		return repository.save(bulkImport);
	}

	@Transactional
	public BulkImport activate(UUID id) {
		List<BulkImport> activeImports = repository.findByStatus(ImportStatus.ACTIVE);
		activeImports.stream().forEach(i -> i.setStatus(ImportStatus.DEPRECATED));
		repository.saveAll(activeImports);
		BulkImport bulkImport = findById(id);
		bulkImport.setStatus(ImportStatus.ACTIVE);
		bulkImport.setActivatedAt(OffsetDateTime.now());
		return repository.save(bulkImport);
	}

	@Transactional
	public void delete(UUID id) {
		repository.delete(findById(id));
	}
}
