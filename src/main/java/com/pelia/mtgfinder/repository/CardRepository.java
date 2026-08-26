package com.pelia.mtgfinder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.Card;
import com.pelia.mtgfinder.model.CardId;

public interface CardRepository extends JpaRepository<Card, CardId> {

	@Query(value = """
			WITH tokenized_cards AS (
				SELECT card.import_id,
				       card.id,
				       COALESCE(
				           CEIL(ARRAY_LENGTH(REGEXP_SPLIT_TO_ARRAY(TRIM(card.context_text), '\\s+'), 1) * 1.35),
				           0
				       )::BIGINT AS estimated_tokens,
				       ROW_NUMBER() OVER (ORDER BY card.id) AS row_number
				FROM cards card
				WHERE card.import_id = :importId
				  AND card.embedding IS NULL
			), ranked_cards AS (
				SELECT tokenized_cards.*,
				       SUM(estimated_tokens) OVER (
				           ORDER BY id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
				       ) AS accumulated_tokens
				FROM tokenized_cards
			)
			SELECT card.*
			FROM cards card
			JOIN ranked_cards ranked
			  ON ranked.import_id = card.import_id
			 AND ranked.id = card.id
			WHERE ranked.accumulated_tokens <= :maxEstimatedTokens
			   OR ranked.row_number = 1
			ORDER BY card.id
			""", nativeQuery = true)
	List<Card> findPendingEmbeddingsByImportId(
			@Param("importId") UUID importId,
			@Param("maxEstimatedTokens") int maxEstimatedTokens);

	List<Card> findByIdImportId(UUID importId);

	List<Card> findByIdImportIdAndNameContainingIgnoreCase(UUID importId, String name);

	List<Card> findByOracleId(UUID oracleId);
}
