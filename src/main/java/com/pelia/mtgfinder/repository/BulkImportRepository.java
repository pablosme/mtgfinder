package com.pelia.mtgfinder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pelia.mtgfinder.model.BulkImport;
import com.pelia.mtgfinder.model.ImportStatus;

public interface BulkImportRepository extends JpaRepository<BulkImport, UUID> {

	List<BulkImport> findByStatus(ImportStatus status);
}
