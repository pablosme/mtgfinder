package com.pelia.mtgfinder.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.ActiveCard;
import com.pelia.mtgfinder.model.CardId;

public interface ActiveCardRepository extends JpaRepository<ActiveCard, CardId> {

	@Query(value = """
			SELECT card.import_id AS "importId",
			       card.id AS "cardId",
			       1 - ((card.embedding::halfvec(1024)) <=> CAST(:embedding AS halfvec(1024))) AS "similarityScore"
			FROM cards_active_v card
			WHERE card.embedding IS NOT NULL
			ORDER BY (card.embedding::halfvec(1024)) <=> CAST(:embedding AS halfvec(1024))
			LIMIT :limit
			""", nativeQuery = true)
	List<VectorMatch> searchByEmbedding(
			@Param("embedding") String embedding,
			@Param("limit") int limit);

	List<ActiveCard> findByNameContainingIgnoreCase(String name);

	List<ActiveCard> findByNameContainingIgnoreCaseOrderByNameAscReleasedAtDesc(String name);

	@Query(value = """
			WITH seed_cards AS (
				SELECT card.import_id, card.id
				FROM cards_active_v card
				WHERE position(lower(:name) in lower(card.name)) > 0
				   OR EXISTS (
						SELECT 1
						FROM card_faces face
						WHERE face.import_id = card.import_id
						  AND face.card_id = card.id
						  AND position(lower(:name) in lower(face.name)) > 0
				   )
			), matching_oracle_ids AS (
				SELECT card.oracle_id
				FROM cards_active_v card
				JOIN seed_cards seed
				  ON seed.import_id = card.import_id
				 AND seed.id = card.id
				WHERE card.oracle_id IS NOT NULL

				UNION

				SELECT face.oracle_id
				FROM card_faces face
				JOIN seed_cards seed
				  ON seed.import_id = face.import_id
				 AND seed.id = face.card_id
				WHERE face.oracle_id IS NOT NULL
			), matching_cards AS (
				SELECT seed.import_id, seed.id
				FROM seed_cards seed

				UNION

				SELECT card.import_id, card.id
				FROM cards_active_v card
				WHERE card.oracle_id IN (SELECT oracle_id FROM matching_oracle_ids)
				   OR EXISTS (
						SELECT 1
						FROM card_faces face
						WHERE face.import_id = card.import_id
						  AND face.card_id = card.id
						  AND face.oracle_id IN (SELECT oracle_id FROM matching_oracle_ids)
				   )
			)
			SELECT card.*
			FROM cards_active_v card
			JOIN matching_cards matching
			  ON matching.import_id = card.import_id
			 AND matching.id = card.id
			ORDER BY card.name ASC, card.released_at DESC
			""", nativeQuery = true)
	List<ActiveCard> searchByNameIncludingFacesAndVariations(@Param("name") String name);

	List<ActiveCard> findByOracleIdInOrderByNameAscReleasedAtDesc(Collection<UUID> oracleIds);

	List<ActiveCard> findByOracleId(UUID oracleId);

	List<ActiveCard> findBySetCodeIgnoreCase(String setCode);

	interface VectorMatch {
		UUID getImportId();
		UUID getCardId();
		Double getSimilarityScore();
	}
}
