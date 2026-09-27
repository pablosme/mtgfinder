package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.model.ActiveCard;
import com.pelia.mtgfinder.repository.ActiveCardRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class ActiveCardService {

	private final ActiveCardRepository repository;

	public ActiveCardService(ActiveCardRepository repository) {
		this.repository = repository;
	}

	public List<ActiveCard> findAll() {
		return repository.findAll();
	}

	public ActiveCard findById(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Active card not found: " + id));
	}

	public List<ActiveCard> findByName(String name) {
		return repository.findByNameContainingIgnoreCase(name);
	}

	public List<ActiveCard> findByOracleId(UUID oracleId) {
		return repository.findByOracleId(oracleId);
	}

	public List<ActiveCard> findBySetCode(String setCode) {
		return repository.findBySetCodeIgnoreCase(setCode);
	}
}
