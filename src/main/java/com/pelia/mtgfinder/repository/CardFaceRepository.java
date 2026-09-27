package com.pelia.mtgfinder.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.CardFace;

public interface CardFaceRepository extends JpaRepository<CardFace, Long> {

	List<CardFace> findByCardIdOrderByFaceIndex(UUID cardId);

	@Query(value = """
			SELECT face.*
			FROM card_faces face
			JOIN cards card ON card.id = face.card_id
			WHERE card.id IN (:cardIds)
			ORDER BY face.card_id, face.face_index
			""", nativeQuery = true)
	List<CardFace> findForActiveCardsByCardIds(@Param("cardIds") Collection<UUID> cardIds);
}
