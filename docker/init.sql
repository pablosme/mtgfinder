-- Proposed PostgreSQL schema: Oracle identity -> print -> nested faces.
-- Fresh-database schema with import tracking, staging, and search indexes.
-- Shared embeddings are queried directly from card_embedding; no views are used.
--
-- Mapping from the supplied Scryfall examples:
--   Thought Vessel: one card_oracle, one card_print per Scryfall id, no card_face
--     rows (Scryfall stores this card's only face directly on the root object).
--   Smoldering Werewolf // Erupting Dreadwolf: one card_oracle, one card_print
--     per Scryfall id, and two card_face rows per print, in source array order.
--   Root gameplay fields go to card_oracle; root print fields go to card_print.
--   Every card_faces element goes to card_face, including its gameplay fields.
--   Face gameplay consequently repeats across prints in this three-table model.
--
-- Import rules:
--   Preserve SQL NULL for absent fields, '' for an explicitly empty mana cost,
--   and '{}' for an explicitly empty array (e.g. colors on a colorless card).
--   Do not copy front-face values into absent root gameplay fields.
--   Preserve root and face image_uris separately; some layouts have root images
--   even when card_faces is present. JSONB retains all supplied image variants.
--   printed_* values are nullable and must not be replaced by Oracle text.
--   Fixed Scryfall object discriminators (card/card_face) are implicit in tables.
--
-- Internal identity: one core row per real root oracle_id, or one per print when
-- the root has no oracle_id. The importer generates a stable internal UUID from
-- a namespaced source key (oracle:<oracle_id> or print:<id>). Face Oracle IDs are
-- preserved independently; exceptions embed the entire print, never each face.

-- 1. Extensions (same dependencies as init.sql).
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Import tracking and logs.
-- Counts refer to Scryfall print IDs, as in the existing API.
CREATE TABLE bulk_imports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scryfall_bulk_type VARCHAR(50) NOT NULL,
    scryfall_updated_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    total_cards INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMPTZ,
    created_cards INTEGER DEFAULT 0,
    updated_cards INTEGER DEFAULT 0,

    CONSTRAINT chk_import_status
        CHECK (status IN ('PROCESSING', 'ACTIVE', 'DEPRECATED', 'FAILED'))
);

CREATE TABLE import_logs (
    id BIGSERIAL PRIMARY KEY,
    import_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message TEXT NOT NULL,

    CONSTRAINT fk_import_logs_import
        FOREIGN KEY (import_id) REFERENCES bulk_imports (id) ON DELETE SET NULL
);

-- 3. Shared Oracle identity and gameplay.
CREATE TABLE card_oracle (
    id UUID PRIMARY KEY,
    oracle_id UUID UNIQUE,
    fallback_print_id UUID UNIQUE,
    CONSTRAINT chk_card_oracle_identity
        CHECK ((oracle_id IS NOT NULL) <> (fallback_print_id IS NOT NULL)),

    -- Shared identity and root gameplay, independent of language and edition.
    name TEXT NOT NULL,
    layout TEXT NOT NULL,
    mana_cost TEXT,
    cmc NUMERIC,
    type_line TEXT,
    oracle_text TEXT,
    power TEXT,
    toughness TEXT,
    loyalty TEXT,
    defense TEXT,
    hand_modifier TEXT,
    life_modifier TEXT,
    colors TEXT[],
    color_identity TEXT[] NOT NULL,
    color_indicator TEXT[],
    keywords TEXT[] NOT NULL,
    produced_mana TEXT[],
    reserved BOOLEAN NOT NULL,
    game_changer BOOLEAN,
    edhrec_rank INTEGER,
    penny_rank INTEGER,
    prints_search_uri TEXT,

    -- Retain the format/status map until other tables are designed.
    legalities JSONB NOT NULL,

    CONSTRAINT chk_card_oracle_legalities_object
        CHECK (jsonb_typeof(legalities) = 'object')
);

-- 4. Prints and their latest successful import.
CREATE TABLE card_print (
    -- Scryfall root id: identifies this exact printing in this language.
    id UUID PRIMARY KEY,
    card_identity_id UUID NOT NULL,
    import_id UUID NOT NULL,
    -- Hash the source card including Oracle/face data, not just print columns,
    -- so created_cards/updated_cards retain their existing whole-card meaning.
    metadata_hash VARCHAR(64),
    -- Prepared during import, before the separate embedding operation runs.
    context_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lang TEXT NOT NULL,
    released_at DATE,

    -- External identifiers belong to the print, even in Scryfall's core block.
    multiverse_ids BIGINT[],
    mtgo_id BIGINT,
    mtgo_foil_id BIGINT,
    arena_id BIGINT,
    tcgplayer_id BIGINT,
    tcgplayer_etched_id BIGINT,
    cardmarket_id BIGINT,
    resource_id TEXT,

    uri TEXT,
    scryfall_uri TEXT,
    rulings_uri TEXT,

    -- Edition data remains here until a separate set table is introduced.
    set_id UUID,
    set_code TEXT NOT NULL, -- Scryfall root field: set.
    set_name TEXT NOT NULL,
    set_type TEXT,
    set_uri TEXT,
    set_search_uri TEXT,
    scryfall_set_uri TEXT,
    collector_number TEXT NOT NULL, -- May include letters and symbols, e.g. 1495★.
    rarity TEXT NOT NULL,

    -- Root presentation fields; multi-face translations stay on card_face.
    printed_name TEXT,
    printed_type_line TEXT,
    printed_text TEXT,
    flavor_name TEXT,
    flavor_text TEXT,
    artist TEXT,
    artist_ids UUID[],
    illustration_id UUID,
    watermark TEXT,
    card_back_id UUID,
    image_uris JSONB,
    highres_image BOOLEAN,
    image_status TEXT,
    image_updated_at TIMESTAMPTZ,

    -- Physical/digital treatment and availability of this print.
    games TEXT[],
    finishes TEXT[],
    foil BOOLEAN,
    nonfoil BOOLEAN,
    oversized BOOLEAN,
    promo BOOLEAN,
    promo_types TEXT[],
    reprint BOOLEAN,
    variation BOOLEAN,
    variation_of UUID,
    digital BOOLEAN,
    border_color TEXT,
    frame TEXT,
    frame_effects TEXT[],
    security_stamp TEXT,
    full_art BOOLEAN,
    textless BOOLEAN,
    booster BOOLEAN,
    story_spotlight BOOLEAN,
    content_warning BOOLEAN,
    attraction_lights SMALLINT[],

    -- Retain ancillary objects until dedicated tables are designed.
    -- all_parts contains print IDs, so it is associated with the source print.
    all_parts JSONB,
    prices JSONB,
    related_uris JSONB,
    purchase_uris JSONB,
    preview JSONB,

    CONSTRAINT fk_card_print_oracle
        FOREIGN KEY (card_identity_id) REFERENCES card_oracle (id),
    CONSTRAINT fk_card_print_import
        FOREIGN KEY (import_id) REFERENCES bulk_imports (id) ON DELETE CASCADE,
    -- Supports group lookup and ensures an embedding's source belongs to its group.
    CONSTRAINT uq_card_print_identity_lang_id
        UNIQUE (card_identity_id, lang, id),
    CONSTRAINT chk_card_print_rarity
        CHECK (rarity IN ('common', 'uncommon', 'rare', 'mythic', 'special', 'bonus')),
    CONSTRAINT chk_card_print_image_uris_object
        CHECK (jsonb_typeof(image_uris) = 'object'),
    CONSTRAINT chk_card_print_all_parts_array
        CHECK (jsonb_typeof(all_parts) = 'array'),
    CONSTRAINT chk_card_print_prices_object
        CHECK (jsonb_typeof(prices) = 'object'),
    CONSTRAINT chk_card_print_related_uris_object
        CHECK (jsonb_typeof(related_uris) = 'object'),
    CONSTRAINT chk_card_print_purchase_uris_object
        CHECK (jsonb_typeof(purchase_uris) = 'object'),
    CONSTRAINT chk_card_print_preview_object
        CHECK (jsonb_typeof(preview) = 'object')
);

-- 5. Faces; import provenance is available through card_print.import_id.
CREATE TABLE card_face (
    card_print_id UUID NOT NULL,
    face_index SMALLINT NOT NULL, -- Zero-based position in Scryfall card_faces.

    -- Gameplay belongs to this particular face, not to the combined root card.
    name TEXT NOT NULL,
    mana_cost TEXT,
    cmc NUMERIC, -- Only populate when supplied for the face; do not copy root cmc.
    type_line TEXT,
    oracle_text TEXT,
    power TEXT,
    toughness TEXT,
    loyalty TEXT,
    defense TEXT,
    colors TEXT[],
    color_indicator TEXT[],

    -- Optional face identity fields, distinct from the parent Oracle identity.
    oracle_id UUID,
    layout TEXT,

    -- Translation and art can differ for each face of each print.
    printed_name TEXT,
    printed_type_line TEXT,
    printed_text TEXT,
    flavor_name TEXT,
    flavor_text TEXT,
    artist TEXT,
    artist_id UUID,
    illustration_id UUID,
    watermark TEXT,
    image_uris JSONB,

    -- A face has no standalone Scryfall print ID; its parent and position identify it.
    PRIMARY KEY (card_print_id, face_index),
    CONSTRAINT fk_card_face_print
        FOREIGN KEY (card_print_id) REFERENCES card_print (id) ON DELETE CASCADE,
    CONSTRAINT chk_card_face_index
        CHECK (face_index >= 0),
    CONSTRAINT chk_card_face_image_uris_object
        CHECK (jsonb_typeof(image_uris) = 'object')
);

-- Parent Oracle identity is reached through card_print.card_identity_id; it is not
-- duplicated in card_face.oracle_id (which stores only the optional source field).
-- No FK is imposed on variation_of or source face oracle_id: referenced objects
-- may be outside an imported subset.

-- 6. One embedding per internal identity/language, retaining the exact source.
-- For real Oracle IDs this shares vectors across prints; exceptions have their
-- own identity and therefore one vector for the entire print.
CREATE TABLE card_embedding (
    card_identity_id UUID NOT NULL,
    lang TEXT NOT NULL,
    card_print_id UUID NOT NULL,
    context_text TEXT NOT NULL,
    embedding vector(1024) NOT NULL,

    PRIMARY KEY (card_identity_id, lang),
    CONSTRAINT fk_card_embedding_identity
        FOREIGN KEY (card_identity_id) REFERENCES card_oracle (id) ON DELETE CASCADE,
    CONSTRAINT fk_card_embedding_source_print
        FOREIGN KEY (card_identity_id, lang, card_print_id)
        REFERENCES card_print (card_identity_id, lang, id) ON DELETE CASCADE
);

-- 7. Bulk COPY staging.
-- LIKE copies column types and NOT NULL requirements. EXCLUDING ALL avoids
-- copying live defaults, CHECK constraints, and indexes; FKs are never copied.
-- Normalize required source fields before COPY. Validate remaining constraints
-- when merging into live tables. Staging deliberately has no foreign keys, so
-- rows can arrive before their live parents. Failed imports must clear staging.
-- UNLOGGED tables are disposable and must be reloaded after crash recovery.

CREATE UNLOGGED TABLE card_oracle_staging (
    import_id UUID NOT NULL,
    source_card_print_id UUID NOT NULL,
    LIKE card_oracle EXCLUDING ALL,

    -- Bulk data repeats Oracle data for every print. Keeping the source print
    -- in the key permits streaming COPY without cross-batch deduplication.
    PRIMARY KEY (import_id, source_card_print_id)
);

CREATE UNLOGGED TABLE card_print_staging (
    LIKE card_print EXCLUDING ALL,

    PRIMARY KEY (import_id, id)
);

-- The live default records first insertion; preserve it on subsequent upserts.
ALTER TABLE card_print_staging DROP COLUMN created_at;

CREATE UNLOGGED TABLE card_face_staging (
    import_id UUID NOT NULL,
    LIKE card_face EXCLUDING ALL,

    PRIMARY KEY (import_id, card_print_id, face_index)
);

-- Import finalization remains an application-managed transaction:
--   1. Count new/changed print IDs using card_print_staging.metadata_hash.
--   2. Select one consistent row per internal identity from card_oracle_staging for the
--      import, using source_card_print_id as a deterministic tie-breaker. Upsert
--      card_oracle first; never send repeated internal IDs to one ON CONFLICT query.
--   3. Upsert card_print by id, then replace card_face rows for the staged prints.
--      Preserve prints absent from this import, matching the existing workflow.
--      If a source print changes Oracle identity or language, delete its embedding
--      before the upsert so the source/group foreign key remains valid.
--   4. For existing embeddings, compare context_text from the saved card_print_id
--      with card_embedding.context_text. Do not select another print for this
--      comparison: a different print's hash does not mean the source changed.
--      A metadata_hash change alone must not trigger regeneration if the actual
--      embedding input is unchanged. Resolve changed shared Oracle/face gameplay
--      into the source print's context_text before comparing.
--      Preserve the saved source while regenerating a changed vector; update its
--      context_text and vector together only after generation succeeds.
--   5. Deprecate the previous ACTIVE import, activate this import with its counts,
--      and clear all three staging tables for its import_id before committing.
-- For groups without an embedding, choose the lowest live print id initially.
-- Generation stores card_print_id, context_text, and the vector together, using
-- ON CONFLICT (card_identity_id, lang) for both initial generation and later refreshes.
-- Pending work includes missing rows and saved sources with changed context_text;
-- semantic search must exclude stale vectors until their refresh completes.
-- No embedding staging table is needed for this separate post-import operation.
-- Deleting the saved source print deletes its embedding, allowing another print
-- in the group to be selected on the next run. Shared Oracle rows are not owned
-- by an import; cleanup of Oracle rows with no prints is an application action.

-- 8. Lookup and relationship indexes.
CREATE INDEX idx_import_logs_import_id ON import_logs (import_id);
CREATE INDEX idx_card_print_import_id ON card_print (import_id);
-- uq_card_print_identity_lang_id already indexes identity lookup, group joins,
-- and initial representative selection within a language.
CREATE INDEX idx_card_oracle_name ON card_oracle (name);
CREATE INDEX idx_card_oracle_cmc ON card_oracle (cmc);
CREATE INDEX idx_card_face_oracle_id ON card_face (oracle_id);
CREATE INDEX idx_card_print_set_code ON card_print (lower(set_code));
-- card_face's primary key already indexes card_print_id; staging primary keys
-- already index import_id for cleanup. card_embedding's key indexes its group.

-- 9. Gameplay filtering on shared Oracle data.
CREATE INDEX idx_card_oracle_colors_gin ON card_oracle USING GIN (colors);
CREATE INDEX idx_card_oracle_color_identity_gin ON card_oracle USING GIN (color_identity);
CREATE INDEX idx_card_oracle_keywords_gin ON card_oracle USING GIN (keywords);
CREATE INDEX idx_card_oracle_legalities_gin ON card_oracle USING GIN (legalities);

-- 10. Fuzzy name search, including localized print and face names.
CREATE INDEX idx_card_oracle_name_trgm
    ON card_oracle USING GIN (lower(name) gin_trgm_ops);
CREATE INDEX idx_card_face_name_trgm
    ON card_face USING GIN (lower(name) gin_trgm_ops);
CREATE INDEX idx_card_print_printed_name_trgm
    ON card_print USING GIN (lower(printed_name) gin_trgm_ops);
CREATE INDEX idx_card_face_printed_name_trgm
    ON card_face USING GIN (lower(printed_name) gin_trgm_ops);

-- 11. Semantic search directly on the unique Oracle/language embedding rows.
-- Queries must use the same halfvec cast and cosine distance operator (<=>).
CREATE INDEX idx_card_embedding_hnsw
    ON card_embedding
    USING hnsw ((embedding::halfvec(1024)) halfvec_cosine_ops)
    WITH (m = 16, ef_construction = 64);
