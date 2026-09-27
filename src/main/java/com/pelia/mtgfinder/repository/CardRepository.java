package com.pelia.mtgfinder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.Card;

public interface CardRepository extends JpaRepository<Card, UUID> {

	@Query(value = """
			WITH pending_cards AS (
				SELECT representative.id
				FROM cards_semantic_v semantic
				CROSS JOIN LATERAL (
					SELECT card.id
					FROM cards card
					WHERE card.oracle_id = semantic.oracle_id
					  AND card.lang = semantic.lang
					ORDER BY card.id
					LIMIT 1
				) representative
				WHERE semantic.embedding IS NULL
				  AND semantic.oracle_id IS NOT NULL

				UNION ALL

				SELECT card.id
				FROM cards card
				WHERE card.oracle_id IS NULL
				  AND card.embedding IS NULL
			), batch AS (
				SELECT id
				FROM pending_cards
				ORDER BY id
				LIMIT :batchSize
			)
			SELECT card.*
			FROM cards card
			JOIN batch ON batch.id = card.id
			ORDER BY card.id
			""", nativeQuery = true)
	List<Card> findPendingEmbeddings(@Param("batchSize") int batchSize);

	List<Card> findByImportId(UUID importId);

	List<Card> findByImportIdAndNameContainingIgnoreCase(UUID importId, String name);

	List<Card> findByOracleId(UUID oracleId);
}
