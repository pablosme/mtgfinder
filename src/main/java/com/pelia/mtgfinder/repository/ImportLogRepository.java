package com.pelia.mtgfinder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pelia.mtgfinder.model.ImportLog;

public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {

    List<ImportLog> findByImportIdOrderByIdAsc(UUID importId);
}
