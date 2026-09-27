package com.pelia.mtgfinder.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.ActiveCard;

public interface ActiveCardRepository extends JpaRepository<ActiveCard, UUID> {

	@Query(value = """
			WITH semantic_candidates AS (
				SELECT semantic.oracle_id, semantic.lang, NULL::uuid AS card_id,
				       (semantic.embedding::halfvec(1024)) <=> CAST(:embedding AS halfvec(1024)) AS distance
				FROM cards_semantic_v semantic
				WHERE semantic.oracle_id IS NOT NULL
				  AND semantic.embedding IS NOT NULL

				UNION ALL

				SELECT card.oracle_id, card.lang, card.id AS card_id,
				       (card.embedding::halfvec(1024)) <=> CAST(:embedding AS halfvec(1024)) AS distance
				FROM cards card
				WHERE card.oracle_id IS NULL
				  AND card.embedding IS NOT NULL
			), semantic_matches AS (
				SELECT *
				FROM semantic_candidates
				ORDER BY distance, oracle_id, lang, card_id
				LIMIT :limit
			)
			SELECT card.id AS "cardId", 1 - matching.distance AS "similarityScore"
			FROM semantic_matches matching
			JOIN cards card
			  ON (card.oracle_id = matching.oracle_id AND card.lang = matching.lang)
			  OR card.id = matching.card_id
			ORDER BY matching.distance, matching.oracle_id, matching.lang,
			         card.name, card.released_at DESC, card.id
			""", nativeQuery = true)
	List<VectorMatch> searchByEmbedding(
			@Param("embedding") String embedding,
			@Param("limit") int limit);

	@Query(value = """
			WITH search_params AS (
				SELECT lower(CAST(:name AS text)) AS query
			), fuzzy_seed_cards AS (
				SELECT card.id,
				       similarity(lower(card.name), params.query) AS similarity_score
				FROM cards card
				CROSS JOIN search_params params
				WHERE lower(card.name) % params.query
				  AND similarity(lower(card.name), params.query) >= :threshold

				UNION ALL

				SELECT face.card_id AS id,
				       similarity(lower(face.name), params.query) AS similarity_score
				FROM card_faces face
				JOIN cards card ON card.id = face.card_id
				CROSS JOIN search_params params
				WHERE lower(face.name) % params.query
				  AND similarity(lower(face.name), params.query) >= :threshold
			), scored_seed_cards AS (
				SELECT id, MAX(similarity_score) AS similarity_score
				FROM fuzzy_seed_cards
				GROUP BY id
			), matching_oracle_scores AS (
				SELECT oracle_id, MAX(similarity_score) AS similarity_score
				FROM (
					SELECT card.oracle_id, seed.similarity_score
					FROM cards card
					JOIN scored_seed_cards seed
					  ON seed.id = card.id
					WHERE card.oracle_id IS NOT NULL

					UNION ALL

					SELECT face.oracle_id, seed.similarity_score
					FROM card_faces face
					JOIN scored_seed_cards seed ON seed.id = face.card_id
					WHERE face.oracle_id IS NOT NULL
				) oracle_matches
				GROUP BY oracle_id
			), expanded_matches AS (
				SELECT seed.id, seed.similarity_score
				FROM scored_seed_cards seed

				UNION ALL

				SELECT card.id, oracle.similarity_score
				FROM cards card
				JOIN matching_oracle_scores oracle
				  ON oracle.oracle_id = card.oracle_id

				UNION ALL

				SELECT card.id, oracle.similarity_score
				FROM cards card
				JOIN card_faces face ON face.card_id = card.id
				JOIN matching_oracle_scores oracle
				  ON oracle.oracle_id = face.oracle_id
			), matching_cards AS (
				SELECT id, MAX(similarity_score) AS similarity_score
				FROM expanded_matches
				GROUP BY id
			)
			SELECT matching.id AS "cardId",
			       matching.similarity_score AS "similarityScore"
			FROM matching_cards matching
			JOIN cards card ON card.id = matching.id
			ORDER BY matching.similarity_score DESC, card.name ASC, card.released_at DESC
			""", nativeQuery = true)
	List<FuzzyMatch> searchByFuzzyName(
			@Param("name") String name,
			@Param("threshold") double threshold);

	List<ActiveCard> findByNameContainingIgnoreCase(String name);

	List<ActiveCard> findByNameContainingIgnoreCaseOrderByNameAscReleasedAtDesc(String name);

	@Query(value = """
			WITH seed_cards AS (
				SELECT card.id
				FROM cards card
				WHERE position(lower(:name) in lower(card.name)) > 0
				   OR EXISTS (
						SELECT 1
						FROM card_faces face
						WHERE face.card_id = card.id
						  AND position(lower(:name) in lower(face.name)) > 0
				   )
			), matching_oracle_ids AS (
				SELECT card.oracle_id
				FROM cards card
				JOIN seed_cards seed ON seed.id = card.id
				WHERE card.oracle_id IS NOT NULL

				UNION

				SELECT face.oracle_id
				FROM card_faces face
				JOIN seed_cards seed ON seed.id = face.card_id
				WHERE face.oracle_id IS NOT NULL
			), matching_cards AS (
				SELECT seed.id
				FROM seed_cards seed

				UNION

				SELECT card.id
				FROM cards card
				WHERE card.oracle_id IN (SELECT oracle_id FROM matching_oracle_ids)
				   OR EXISTS (
						SELECT 1
						FROM card_faces face
						WHERE face.card_id = card.id
						  AND face.oracle_id IN (SELECT oracle_id FROM matching_oracle_ids)
				   )
			)
			SELECT card.*
			FROM cards card
			JOIN matching_cards matching ON matching.id = card.id
			ORDER BY card.name ASC, card.released_at DESC
			""", nativeQuery = true)
	List<ActiveCard> searchByNameIncludingFacesAndVariations(@Param("name") String name);

	List<ActiveCard> findByOracleIdInOrderByNameAscReleasedAtDesc(Collection<UUID> oracleIds);

	List<ActiveCard> findByOracleId(UUID oracleId);

	List<ActiveCard> findBySetCodeIgnoreCase(String setCode);

	interface VectorMatch {
		UUID getCardId();
		Double getSimilarityScore();
	}

	interface FuzzyMatch {
		UUID getCardId();
		Double getSimilarityScore();
	}
}
