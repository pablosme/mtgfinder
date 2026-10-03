package com.pelia.mtgfinder.repository;

/** Shared bound predicates, always applied before ranking limits. */
public final class CardSearchFilterSql {
    private CardSearchFilterSql() {}

    // Convert lists to typed arrays so Hibernate binds each as one PostgreSQL array, including empty lists.
    public static final String PREDICATE = """
            AND EXISTS (
                SELECT 1 FROM (SELECT
                    CAST(:#{#filters.type().toArray(new String[0])} AS text[]) AS types,
                    CAST(:#{#filters.keyword().toArray(new String[0])} AS text[]) AS keywords,
                    CAST(:#{#filters.colors().toArray(new String[0])} AS text[]) AS colors,
                    CAST(:#{#filters.colorIdentity().toArray(new String[0])} AS text[]) AS color_identities,
                    CAST(:#{#filters.legality().toArray(new String[0])} AS text[]) AS legalities,
                    CAST(:#{#filters.rarity().toArray(new String[0])} AS text[]) AS rarities,
                    CAST(:#{#filters.set().toArray(new String[0])} AS text[]) AS sets,
                    CAST(:#{#filters.collection().toArray(new String[0])} AS text[]) AS collections,
                    CAST(:#{#filters.cmc().toArray(new java.math.BigDecimal[0])} AS numeric[]) AS mana_values,
                    CAST(:#{#filters.layout().toArray(new String[0])} AS text[]) AS layouts,
                    CAST(:#{#filters.power().toArray(new String[0])} AS text[]) AS powers,
                    CAST(:#{#filters.toughness().toArray(new String[0])} AS text[]) AS toughnesses,
                    CAST(:#{#filters.loyalty().toArray(new String[0])} AS text[]) AS loyalties
                ) filters
                WHERE (cardinality(filters.types) = 0 OR EXISTS (
                    SELECT 1 FROM unnest(filters.types) value
                    WHERE position(lower(value) in lower(oracle.type_line)) > 0
                       OR EXISTS (
                           SELECT 1 FROM card_face face
                           JOIN card_print card ON card.id = face.card_print_id
                           WHERE card.card_identity_id = oracle.id
                             AND position(lower(value) in lower(face.type_line)) > 0)))
                AND (cardinality(filters.keywords) = 0 OR EXISTS (
                    SELECT 1 FROM unnest(oracle.keywords) keyword
                    WHERE lower(keyword) IN (SELECT lower(value) FROM unnest(filters.keywords) value)))
                AND (cardinality(filters.colors) = 0 OR (
                    SELECT CASE WHEN 'C' = ANY(filters.colors) THEN cardinality(available.colors) = 0
                        ELSE available.colors @> filters.colors END
                    FROM (SELECT COALESCE(oracle.colors, (
                        SELECT array_remove(array_agg(DISTINCT color), NULL)
                        FROM card_face face
                        JOIN card_print card ON card.id = face.card_print_id
                        LEFT JOIN LATERAL unnest(face.colors) color ON true
                        WHERE card.card_identity_id = oracle.id AND face.colors IS NOT NULL)) AS colors) available))
                AND (cardinality(filters.color_identities) = 0
                    OR CASE WHEN 'C' = ANY(filters.color_identities) THEN cardinality(oracle.color_identity) = 0
                        ELSE oracle.color_identity @> filters.color_identities END)
                AND (cardinality(filters.legalities) = 0 OR EXISTS (
                    SELECT 1 FROM unnest(filters.legalities) value
                    WHERE oracle.legalities ->> split_part(value, ':', 1) = split_part(value, ':', 2)))
                AND (cardinality(filters.mana_values) = 0 OR oracle.cmc = ANY(filters.mana_values))
                AND (cardinality(filters.layouts) = 0
                    OR lower(oracle.layout) IN (SELECT lower(value) FROM unnest(filters.layouts) value))
                AND ((
                    (cardinality(filters.powers) = 0
                        OR lower(oracle.power) IN (SELECT lower(value) FROM unnest(filters.powers) value))
                    AND (cardinality(filters.toughnesses) = 0
                        OR lower(oracle.toughness) IN (SELECT lower(value) FROM unnest(filters.toughnesses) value))
                    AND (cardinality(filters.loyalties) = 0
                        OR lower(oracle.loyalty) IN (SELECT lower(value) FROM unnest(filters.loyalties) value))
                    ) OR EXISTS (
                        SELECT 1 FROM card_face face
                        JOIN card_print card ON card.id = face.card_print_id
                        WHERE card.card_identity_id = oracle.id
                          AND (cardinality(filters.powers) = 0
                              OR lower(face.power) IN (SELECT lower(value) FROM unnest(filters.powers) value))
                          AND (cardinality(filters.toughnesses) = 0
                              OR lower(face.toughness) IN (SELECT lower(value) FROM unnest(filters.toughnesses) value))
                          AND (cardinality(filters.loyalties) = 0
                              OR lower(face.loyalty) IN (SELECT lower(value) FROM unnest(filters.loyalties) value))))
                AND EXISTS (
                    SELECT 1 FROM card_print card WHERE card.card_identity_id = oracle.id
                      AND (cardinality(filters.rarities) = 0 OR lower(card.rarity) = ANY(filters.rarities))
                      AND (cardinality(filters.sets) = 0 OR EXISTS (
                          SELECT 1 FROM unnest(filters.sets) value
                          WHERE lower(card.set_code) = lower(value)
                             OR position(lower(value) in lower(card.set_name)) > 0))
                      AND (cardinality(filters.collections) = 0 OR EXISTS (
                          SELECT 1 FROM unnest(filters.collections) value
                          WHERE lower(card.set_code) = lower(value)
                             OR position(lower(value) in lower(card.set_name)) > 0
                             OR lower(card.collector_number) = lower(value))))
            )
            """;
}
