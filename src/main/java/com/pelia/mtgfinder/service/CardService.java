package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.model.Card;
import com.pelia.mtgfinder.model.CardId;
import com.pelia.mtgfinder.repository.CardRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class CardService {

	private final CardRepository repository;

	public CardService(CardRepository repository) {
		this.repository = repository;
	}

	public List<Card> findAll() {
		return repository.findAll();
	}

	public Card findById(CardId id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Card not found: " + id.getId()));
	}

	public List<Card> findByImportId(UUID importId) {
		return repository.findByIdImportId(importId);
	}

	public List<Card> findByName(UUID importId, String name) {
		return repository.findByIdImportIdAndNameContainingIgnoreCase(importId, name);
	}

	public List<Card> findByOracleId(UUID oracleId) {
		return repository.findByOracleId(oracleId);
	}

	@Transactional
	public Card save(Card card) {
		return repository.save(card);
	}

	@Transactional
	public void delete(CardId id) {
		repository.delete(findById(id));
	}
}
