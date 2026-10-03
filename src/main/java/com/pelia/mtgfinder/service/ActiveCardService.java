package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.model.Card;
import com.pelia.mtgfinder.repository.CardRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class ActiveCardService {

	private final CardRepository repository;

	public ActiveCardService(CardRepository repository) {
		this.repository = repository;
	}

	public List<Card> findAll() {
		return repository.findAll();
	}

	public Card findById(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Active card not found: " + id));
	}

	public List<Card> findByName(String name) {
		return repository.findByNameContainingIgnoreCase(name);
	}

	public List<Card> findByOracleId(UUID oracleId) {
		return repository.findByOracleId(oracleId);
	}

	public List<Card> findBySetCode(String setCode) {
		return repository.findBySetCodeIgnoreCase(setCode);
	}
}
