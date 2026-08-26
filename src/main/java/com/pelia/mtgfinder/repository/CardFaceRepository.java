package com.pelia.mtgfinder.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.CardFace;

public interface CardFaceRepository extends JpaRepository<CardFace, Long> {

	List<CardFace> findByImportIdAndCardIdOrderByFaceIndex(UUID importId, UUID cardId);

	@Query(value = """
			SELECT face.*
			FROM card_faces face
			JOIN cards_active_v card
			  ON card.import_id = face.import_id
			 AND card.id = face.card_id
			WHERE card.id IN (:cardIds)
			ORDER BY face.import_id, face.card_id, face.face_index
			""", nativeQuery = true)
	List<CardFace> findForActiveCardsByCardIds(@Param("cardIds") Collection<UUID> cardIds);
}
