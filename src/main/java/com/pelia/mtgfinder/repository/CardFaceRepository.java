package com.pelia.mtgfinder.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.CardFace;
import com.pelia.mtgfinder.model.CardFaceId;

public interface CardFaceRepository extends JpaRepository<CardFace, CardFaceId> {

	List<CardFace> findByCardPrintIdOrderByFaceIndex(UUID cardId);

	@Query(value = """
			SELECT face.*
			FROM card_face face
			WHERE face.card_print_id IN (:cardPrintIds)
			ORDER BY face.card_print_id, face.face_index
			""", nativeQuery = true)
	List<CardFace> findByCardPrintIds(@Param("cardPrintIds") Collection<UUID> cardPrintIds);
}
