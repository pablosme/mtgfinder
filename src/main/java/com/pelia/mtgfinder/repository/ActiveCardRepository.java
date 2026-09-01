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

	@Query(value = """
			WITH search_params AS (
				SELECT lower(CAST(:name AS text)) AS query
			), fuzzy_seed_cards AS (
				SELECT card.import_id,
				       card.id,
				       similarity(lower(card.name), params.query) AS similarity_score
				FROM cards_active_v card
				CROSS JOIN search_params params
				WHERE lower(card.name) % params.query
				  AND similarity(lower(card.name), params.query) >= :threshold

				UNION ALL

				SELECT face.import_id,
				       face.card_id AS id,
				       similarity(lower(face.name), params.query) AS similarity_score
				FROM card_faces face
				JOIN cards_active_v card
				  ON card.import_id = face.import_id
				 AND card.id = face.card_id
				CROSS JOIN search_params params
				WHERE lower(face.name) % params.query
				  AND similarity(lower(face.name), params.query) >= :threshold
			), scored_seed_cards AS (
				SELECT import_id, id, MAX(similarity_score) AS similarity_score
				FROM fuzzy_seed_cards
				GROUP BY import_id, id
			), matching_oracle_scores AS (
				SELECT oracle_id, MAX(similarity_score) AS similarity_score
				FROM (
					SELECT card.oracle_id, seed.similarity_score
					FROM cards_active_v card
					JOIN scored_seed_cards seed
					  ON seed.import_id = card.import_id
					 AND seed.id = card.id
					WHERE card.oracle_id IS NOT NULL

					UNION ALL

					SELECT face.oracle_id, seed.similarity_score
					FROM card_faces face
					JOIN scored_seed_cards seed
					  ON seed.import_id = face.import_id
					 AND seed.id = face.card_id
					WHERE face.oracle_id IS NOT NULL
				) oracle_matches
				GROUP BY oracle_id
			), expanded_matches AS (
				SELECT seed.import_id, seed.id, seed.similarity_score
				FROM scored_seed_cards seed

				UNION ALL

				SELECT card.import_id, card.id, oracle.similarity_score
				FROM cards_active_v card
				JOIN matching_oracle_scores oracle
				  ON oracle.oracle_id = card.oracle_id

				UNION ALL

				SELECT card.import_id, card.id, oracle.similarity_score
				FROM cards_active_v card
				JOIN card_faces face
				  ON face.import_id = card.import_id
				 AND face.card_id = card.id
				JOIN matching_oracle_scores oracle
				  ON oracle.oracle_id = face.oracle_id
			), matching_cards AS (
				SELECT import_id, id, MAX(similarity_score) AS similarity_score
				FROM expanded_matches
				GROUP BY import_id, id
			)
			SELECT matching.import_id AS "importId",
			       matching.id AS "cardId",
			       matching.similarity_score AS "similarityScore"
			FROM matching_cards matching
			JOIN cards_active_v card
			  ON card.import_id = matching.import_id
			 AND card.id = matching.id
			ORDER BY matching.similarity_score DESC, card.name ASC, card.released_at DESC
			""", nativeQuery = true)
	List<FuzzyMatch> searchByFuzzyName(
			@Param("name") String name,
			@Param("threshold") double threshold);

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

	interface FuzzyMatch {
		UUID getImportId();
		UUID getCardId();
		Double getSimilarityScore();
	}
}
