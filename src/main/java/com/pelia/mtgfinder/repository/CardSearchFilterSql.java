package com.pelia.mtgfinder.repository;

/** Shared bound predicates, always applied before ranking limits. */
public final class CardSearchFilterSql {
    private CardSearchFilterSql() {}

    public static final String PREDICATE = """
            AND (CAST(:#{#filters.type()} AS text) IS NULL
                 OR position(lower(CAST(:#{#filters.type()} AS text)) in lower(oracle.type_line)) > 0
                 OR EXISTS (
                     SELECT 1 FROM card_face face
                     JOIN card_print card ON card.id = face.card_print_id
                     WHERE card.card_identity_id = oracle.id
                       AND position(lower(CAST(:#{#filters.type()} AS text)) in lower(face.type_line)) > 0))
            AND (CAST(:#{#filters.keyword()} AS text) IS NULL OR EXISTS (
                 SELECT 1 FROM unnest(oracle.keywords) keyword
                 WHERE lower(keyword) = lower(CAST(:#{#filters.keyword()} AS text))))
            AND (CAST(:#{#filters.colors()} AS text) IS NULL OR (
                 SELECT CASE WHEN CAST(:#{#filters.colors()} AS text) = 'C'
                     THEN cardinality(available.colors) = 0
                     ELSE available.colors @> string_to_array(CAST(:#{#filters.colors()} AS text), ',') END
                 FROM (SELECT COALESCE(oracle.colors, (
                     SELECT array_remove(array_agg(DISTINCT color), NULL)
                     FROM card_face face
                     JOIN card_print card ON card.id = face.card_print_id
                     LEFT JOIN LATERAL unnest(face.colors) color ON true
                     WHERE card.card_identity_id = oracle.id AND face.colors IS NOT NULL)) AS colors) available))
            AND (CAST(:#{#filters.colorIdentity()} AS text) IS NULL
                 OR CASE WHEN CAST(:#{#filters.colorIdentity()} AS text) = 'C'
                     THEN cardinality(oracle.color_identity) = 0
                     ELSE oracle.color_identity @> string_to_array(CAST(:#{#filters.colorIdentity()} AS text), ',') END)
            AND (CAST(:#{#filters.legalityFormat()} AS text) IS NULL
                 OR oracle.legalities ->> CAST(:#{#filters.legalityFormat()} AS text)
                     = CAST(:#{#filters.legalityStatus()} AS text))
            AND (CAST(:#{#filters.cmc()} AS numeric) IS NULL OR oracle.cmc = CAST(:#{#filters.cmc()} AS numeric))
            AND (CAST(:#{#filters.layout()} AS text) IS NULL
                 OR lower(oracle.layout) = lower(CAST(:#{#filters.layout()} AS text)))
            AND ((
                 (CAST(:#{#filters.power()} AS text) IS NULL OR lower(oracle.power) = lower(CAST(:#{#filters.power()} AS text)))
                 AND (CAST(:#{#filters.toughness()} AS text) IS NULL OR lower(oracle.toughness) = lower(CAST(:#{#filters.toughness()} AS text)))
                 AND (CAST(:#{#filters.loyalty()} AS text) IS NULL OR lower(oracle.loyalty) = lower(CAST(:#{#filters.loyalty()} AS text)))
                 ) OR EXISTS (
                     SELECT 1 FROM card_face face
                     JOIN card_print card ON card.id = face.card_print_id
                     WHERE card.card_identity_id = oracle.id
                       AND (CAST(:#{#filters.power()} AS text) IS NULL OR lower(face.power) = lower(CAST(:#{#filters.power()} AS text)))
                       AND (CAST(:#{#filters.toughness()} AS text) IS NULL OR lower(face.toughness) = lower(CAST(:#{#filters.toughness()} AS text)))
                       AND (CAST(:#{#filters.loyalty()} AS text) IS NULL OR lower(face.loyalty) = lower(CAST(:#{#filters.loyalty()} AS text)))))
            AND EXISTS (
                 SELECT 1 FROM card_print card WHERE card.card_identity_id = oracle.id
                   AND (CAST(:#{#filters.rarity()} AS text) IS NULL OR lower(card.rarity) = CAST(:#{#filters.rarity()} AS text))
                   AND (CAST(:#{#filters.set()} AS text) IS NULL
                        OR lower(card.set_code) = lower(CAST(:#{#filters.set()} AS text))
                        OR position(lower(CAST(:#{#filters.set()} AS text)) in lower(card.set_name)) > 0)
                   AND (CAST(:#{#filters.collection()} AS text) IS NULL
                        OR lower(card.set_code) = lower(CAST(:#{#filters.collection()} AS text))
                        OR position(lower(CAST(:#{#filters.collection()} AS text)) in lower(card.set_name)) > 0
                        OR lower(card.collector_number) = lower(CAST(:#{#filters.collection()} AS text))))
            """;
}
