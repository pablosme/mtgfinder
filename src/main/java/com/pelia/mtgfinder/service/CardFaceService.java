package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.model.CardFace;
import com.pelia.mtgfinder.repository.CardFaceRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class CardFaceService {

	private final CardFaceRepository repository;

	public CardFaceService(CardFaceRepository repository) {
		this.repository = repository;
	}

	public List<CardFace> findAll() {
		return repository.findAll();
	}

	public CardFace findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Card face not found: " + id));
	}

	public List<CardFace> findByCard(UUID importId, UUID cardId) {
		return repository.findByImportIdAndCardIdOrderByFaceIndex(importId, cardId);
	}

	@Transactional
	public CardFace save(CardFace cardFace) {
		return repository.save(cardFace);
	}

	@Transactional
	public void delete(Long id) {
		repository.delete(findById(id));
	}
}
