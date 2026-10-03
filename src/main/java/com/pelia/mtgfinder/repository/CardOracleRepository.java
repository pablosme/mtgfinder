package com.pelia.mtgfinder.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.CardOracle;
import com.pelia.mtgfinder.dto.CardSearchFilters;

public interface CardOracleRepository extends JpaRepository<CardOracle, UUID> {

    @Query("""
            select o from CardOracle o
            where o.oracleId = :oracleId or (o.oracleId is null and o.id = :oracleId)
            """)
    Optional<CardOracle> findForInfo(@Param("oracleId") UUID oracleId);

    @Query(value = """
            WITH matching_identities AS (
                SELECT oracle.id
                FROM card_oracle oracle
                WHERE position(lower(:name) in lower(oracle.name)) > 0
                  AND EXISTS (SELECT 1 FROM card_print card WHERE card.card_identity_id = oracle.id)

                UNION

                SELECT card.card_identity_id
                FROM card_print card
                WHERE position(lower(:name) in lower(card.printed_name)) > 0

                UNION

                SELECT card.card_identity_id
                FROM card_face face
                JOIN card_print card ON card.id = face.card_print_id
                WHERE position(lower(:name) in lower(face.name)) > 0
                   OR position(lower(:name) in lower(face.printed_name)) > 0
            )
            SELECT oracle.*
            FROM card_oracle oracle
            JOIN matching_identities matching ON matching.id = oracle.id
            WHERE true
            """ + CardSearchFilterSql.PREDICATE + """
            ORDER BY oracle.name, oracle.id
            """, nativeQuery = true)
    List<CardOracle> searchByName(@Param("name") String name, @Param("filters") CardSearchFilters filters);

    @Query(value = """
            WITH search_params AS (
                SELECT lower(CAST(:name AS text)) AS query
            ), matching_scores AS (
                SELECT oracle.id, similarity(lower(oracle.name), params.query) AS similarity_score
                FROM card_oracle oracle
                CROSS JOIN search_params params
                WHERE lower(oracle.name) % params.query
                  AND similarity(lower(oracle.name), params.query) >= :threshold
                  AND EXISTS (SELECT 1 FROM card_print card WHERE card.card_identity_id = oracle.id)

                UNION ALL

                SELECT card.card_identity_id, similarity(lower(card.printed_name), params.query)
                FROM card_print card
                CROSS JOIN search_params params
                WHERE lower(card.printed_name) % params.query
                  AND similarity(lower(card.printed_name), params.query) >= :threshold

                UNION ALL

                SELECT card.card_identity_id, similarity(lower(face.name), params.query)
                FROM card_face face
                JOIN card_print card ON card.id = face.card_print_id
                CROSS JOIN search_params params
                WHERE lower(face.name) % params.query
                  AND similarity(lower(face.name), params.query) >= :threshold

                UNION ALL

                SELECT card.card_identity_id, similarity(lower(face.printed_name), params.query)
                FROM card_face face
                JOIN card_print card ON card.id = face.card_print_id
                CROSS JOIN search_params params
                WHERE lower(face.printed_name) % params.query
                  AND similarity(lower(face.printed_name), params.query) >= :threshold
            )
            SELECT oracle.id AS "cardIdentityId", MAX(matching.similarity_score) AS "similarityScore"
            FROM matching_scores matching
            JOIN card_oracle oracle ON oracle.id = matching.id
            WHERE true
            """ + CardSearchFilterSql.PREDICATE + """
            GROUP BY oracle.id, oracle.name
            ORDER BY MAX(matching.similarity_score) DESC, oracle.name, oracle.id
            """, nativeQuery = true)
    List<OracleSearchMatch> searchByFuzzyName(@Param("name") String name, @Param("threshold") double threshold,
            @Param("filters") CardSearchFilters filters);

    @Query(value = """
            SELECT oracle.id AS "cardIdentityId",
                   1 - MIN((embedding.embedding::halfvec(1024)) <=> CAST(:embedding AS halfvec(1024)))
                       AS "similarityScore"
            FROM card_embedding embedding
            JOIN card_print source ON source.id = embedding.card_print_id
            JOIN card_oracle oracle ON oracle.id = embedding.card_identity_id
            WHERE source.context_text = embedding.context_text
            """ + CardSearchFilterSql.PREDICATE + """
            GROUP BY oracle.id, oracle.name
            ORDER BY MIN((embedding.embedding::halfvec(1024)) <=> CAST(:embedding AS halfvec(1024))),
                     oracle.name, oracle.id
            LIMIT :limit
            """, nativeQuery = true)
    List<OracleSearchMatch> searchByEmbedding(@Param("embedding") String embedding, @Param("limit") int limit,
            @Param("filters") CardSearchFilters filters);
}
