-- 1. Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Version/Ingestion Control
CREATE TABLE IF NOT EXISTS bulk_imports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scryfall_bulk_type VARCHAR(50) NOT NULL,
    scryfall_updated_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    total_cards INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMPTZ,
    created_cards INTEGER DEFAULT 0,
    updated_cards INTEGER DEFAULT 0,

    CONSTRAINT chk_import_status CHECK (status IN ('PROCESSING', 'ACTIVE', 'DEPRECATED', 'FAILED'))
);

-- 3. Import Logs
CREATE TABLE IF NOT EXISTS import_logs (
    id BIGSERIAL PRIMARY KEY,
    import_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message TEXT NOT NULL,

    CONSTRAINT fk_import_logs_import
        FOREIGN KEY (import_id)
        REFERENCES bulk_imports(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_import_logs_import_id ON import_logs(import_id);

-- 4. Cards Table
CREATE TABLE IF NOT EXISTS cards (
    id UUID NOT NULL,
    import_id UUID NOT NULL,
    oracle_id UUID,
    metadata_hash VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    lang VARCHAR(10) NOT NULL DEFAULT 'en',
    layout VARCHAR(50) NOT NULL,

    -- Gameplay and Rulings
    mana_cost VARCHAR(100),
    cmc NUMERIC(12, 2),
    type_line VARCHAR(255),
    oracle_text TEXT,
    colors VARCHAR(5)[],
    color_identity VARCHAR(5)[] NOT NULL,
    keywords TEXT[] NOT NULL DEFAULT '{}',
    produced_mana VARCHAR(5)[],

    -- Combat Attributes
    power VARCHAR(30),
    toughness VARCHAR(30),
    loyalty VARCHAR(30),
    defense VARCHAR(30),

    -- Flags
    reserved BOOLEAN NOT NULL DEFAULT FALSE,
    digital BOOLEAN NOT NULL DEFAULT FALSE,
    reprint BOOLEAN NOT NULL DEFAULT FALSE,

    -- Collection
    set_code VARCHAR(10) NOT NULL,
    set_name VARCHAR(255) NOT NULL,
    collector_number VARCHAR(20) NOT NULL,
    rarity VARCHAR(20) NOT NULL,
    flavor_text TEXT,
    artist VARCHAR(255),
    released_at DATE,

    -- Semistructured Data
    legalities JSONB NOT NULL DEFAULT '{}'::jsonb,
    image_uris JSONB,
    prices JSONB NOT NULL DEFAULT '{}'::jsonb,
    related_uris JSONB,
    all_parts JSONB,

    -- Embeddings
    context_text TEXT,
    embedding vector(1024),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Primary Key and Foreign Key Constraints
    PRIMARY KEY (id),
    CONSTRAINT fk_cards_import FOREIGN KEY (import_id) REFERENCES bulk_imports(id) ON DELETE CASCADE,
    CONSTRAINT chk_rarity CHECK (rarity IN ('common', 'uncommon', 'rare', 'mythic', 'special', 'bonus'))
);

-- 5. Faces Table (for multi-faced cards)
CREATE TABLE IF NOT EXISTS card_faces (
    id BIGSERIAL PRIMARY KEY,
    import_id UUID NOT NULL,
    card_id UUID NOT NULL,
    face_index SMALLINT NOT NULL DEFAULT 0,
    
    name VARCHAR(255) NOT NULL,
    oracle_id UUID,
    mana_cost VARCHAR(100),
    cmc NUMERIC(12, 2),
    type_line VARCHAR(255),
    oracle_text TEXT,
    colors VARCHAR(5)[],

    power VARCHAR(30),
    toughness VARCHAR(30),
    loyalty VARCHAR(30),
    defense VARCHAR(30),

    flavor_text TEXT,
    artist VARCHAR(255),
    image_uris JSONB,

    CONSTRAINT fk_card_faces_card
        FOREIGN KEY (card_id)
        REFERENCES cards(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_card_face
        UNIQUE (card_id, face_index)
);


-- 6. Cards Staging Table
CREATE UNLOGGED TABLE IF NOT EXISTS cards_staging (
    id UUID NOT NULL,
    import_id UUID NOT NULL,
    oracle_id UUID,
    metadata_hash VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    lang VARCHAR(10) DEFAULT 'en',
    layout VARCHAR(50),

    -- Gameplay and Rulings
    mana_cost VARCHAR(100),
    cmc NUMERIC(12, 2),
    type_line VARCHAR(255),
    oracle_text TEXT,
    colors VARCHAR(5)[],
    color_identity VARCHAR(5)[],
    keywords TEXT[],
    produced_mana VARCHAR(5)[],

    -- Combat Attributes
    power VARCHAR(30),
    toughness VARCHAR(30),
    loyalty VARCHAR(30),
    defense VARCHAR(30),

    -- Flags
    reserved BOOLEAN,
    digital BOOLEAN,
    reprint BOOLEAN,

    -- Collection
    set_code VARCHAR(10),
    set_name VARCHAR(255),
    collector_number VARCHAR(20),
    rarity VARCHAR(20),
    flavor_text TEXT,
    artist VARCHAR(255),
    released_at DATE,

    -- Semistructured Data
    legalities JSONB,
    image_uris JSONB,
    prices JSONB,
    related_uris JSONB,
    all_parts JSONB,
    context_text TEXT,

    PRIMARY KEY (import_id, id)
);

-- 7. Card Faces Staging Table
CREATE UNLOGGED TABLE IF NOT EXISTS card_faces_staging (
    import_id UUID NOT NULL,
    card_id UUID NOT NULL,
    face_index SMALLINT NOT NULL,

    name VARCHAR(255) NOT NULL,
    oracle_id UUID,
    mana_cost VARCHAR(100),
    cmc NUMERIC(12, 2),
    type_line VARCHAR(255),
    oracle_text TEXT,
    colors VARCHAR(5)[],

    power VARCHAR(30),
    toughness VARCHAR(30),
    loyalty VARCHAR(30),
    defense VARCHAR(30),

    flavor_text TEXT,
    artist VARCHAR(255),
    image_uris JSONB,

    -- Composite key avoids sequence overhead during bulk loading.
    PRIMARY KEY (import_id, card_id, face_index)
);


-- 8. Semantic View
CREATE OR REPLACE VIEW cards_semantic_v AS
SELECT DISTINCT ON (oracle_id, lang)
    oracle_id,
    lang,
    embedding
FROM cards
ORDER BY 
    oracle_id, 
    lang, 
    CASE WHEN embedding IS NOT NULL THEN 0 ELSE 1 END;


------------------------------------------------------------------


-- 1. Indexes
CREATE INDEX IF NOT EXISTS idx_cards_import_oracle ON cards(oracle_id);
CREATE INDEX IF NOT EXISTS idx_cards_import_name ON cards(name);
CREATE INDEX IF NOT EXISTS idx_cards_import_cmc ON cards(cmc);
CREATE INDEX IF NOT EXISTS idx_card_faces_import_oracle ON card_faces(oracle_id);

-- 2. GIN Indexes for Arrays and JSONB
CREATE INDEX IF NOT EXISTS idx_cards_colors_gin ON cards USING GIN(colors);
CREATE INDEX IF NOT EXISTS idx_cards_color_identity_gin ON cards USING GIN(color_identity);
CREATE INDEX IF NOT EXISTS idx_cards_keywords_gin ON cards USING GIN(keywords);
CREATE INDEX IF NOT EXISTS idx_cards_legalities_gin ON cards USING GIN(legalities);

-- 3. GIN Indexes for Fuzzy Search by Name
CREATE INDEX IF NOT EXISTS idx_cards_name_trgm
ON cards USING GIN (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_card_faces_name_trgm
ON card_faces USING GIN (lower(name) gin_trgm_ops);

-- 4. HNSW Index for Vector Search
CREATE INDEX IF NOT EXISTS idx_cards_embedding_hnsw 
ON cards 
USING hnsw ((embedding::halfvec(1024)) halfvec_cosine_ops)
WITH (m = 16, ef_construction = 64);
