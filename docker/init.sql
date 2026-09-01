-- 1. Extensões
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Controle de Versão / Ingestão
CREATE TABLE IF NOT EXISTS bulk_imports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scryfall_bulk_type VARCHAR(50) NOT NULL,            -- ex: 'default-cards', 'oracle-cards'
    scryfall_updated_at TIMESTAMPTZ,                    -- Timestamp informado pelo Scryfall
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',   -- 'PROCESSING', 'ACTIVE', 'DEPRECATED', 'FAILED'
    total_cards INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMPTZ,

    CONSTRAINT chk_import_status CHECK (status IN ('PROCESSING', 'ACTIVE', 'DEPRECATED', 'FAILED'))
);

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

-- 3. Tabela de Cartas (Versionada)
CREATE TABLE IF NOT EXISTS cards (
    id UUID NOT NULL,                                   -- ID da impressão no Scryfall
    import_id UUID NOT NULL,                            -- Versão da carga de dados
    oracle_id UUID,
    name VARCHAR(255) NOT NULL,
    lang VARCHAR(10) NOT NULL DEFAULT 'en',
    layout VARCHAR(50) NOT NULL,
    
    -- IDs de plataformas externas
    multiverse_ids INTEGER[],
    mtgo_id INTEGER,
    arena_id INTEGER,
    tcgplayer_id INTEGER,
    cardmarket_id INTEGER,

    -- Gameplay e Regras
    mana_cost VARCHAR(100),
    cmc NUMERIC(12, 2),
    type_line VARCHAR(255),
    oracle_text TEXT,
    colors VARCHAR(5)[],
    color_identity VARCHAR(5)[] NOT NULL,
    keywords TEXT[] NOT NULL DEFAULT '{}',
    produced_mana VARCHAR(5)[],
    
    -- Atributos de Combate (Sem restrição de 3 dígitos)
    power VARCHAR(30),
    toughness VARCHAR(30),
    loyalty VARCHAR(30),
    defense VARCHAR(30),

    -- Flags
    reserved BOOLEAN NOT NULL DEFAULT FALSE,
    digital BOOLEAN NOT NULL DEFAULT FALSE,
    reprint BOOLEAN NOT NULL DEFAULT FALSE,

    -- Coleção
    set_code VARCHAR(10) NOT NULL,
    set_name VARCHAR(255) NOT NULL,
    collector_number VARCHAR(20) NOT NULL,
    rarity VARCHAR(20) NOT NULL,
    flavor_text TEXT,
    artist VARCHAR(255),
    released_at DATE,

    -- Dados Semiestruturados
    legalities JSONB NOT NULL DEFAULT '{}'::jsonb,
    image_uris JSONB,
    prices JSONB NOT NULL DEFAULT '{}'::jsonb,
    related_uris JSONB,
    all_parts JSONB,

    -- Embeddings
    context_text TEXT,
    embedding vector(1024),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Chave primária composta garantindo isolamento por versão
    PRIMARY KEY (import_id, id),
    CONSTRAINT fk_cards_import FOREIGN KEY (import_id) REFERENCES bulk_imports(id) ON DELETE CASCADE,
    CONSTRAINT chk_rarity CHECK (rarity IN ('common', 'uncommon', 'rare', 'mythic', 'special', 'bonus'))
);

-- 4. Tabela de Faces (Versionada)
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
        FOREIGN KEY (import_id, card_id) 
        REFERENCES cards(import_id, id) 
        ON DELETE CASCADE,
    CONSTRAINT uq_card_face 
        UNIQUE (import_id, card_id, face_index)
);

-- 5. View para consumo da aplicação (Lê apenas a versão ativa)
CREATE OR REPLACE VIEW cards_active_v AS
SELECT c.* 
FROM cards c
JOIN bulk_imports b ON c.import_id = b.id
WHERE b.status = 'ACTIVE';


------------------------------------------------------------------


-- 1. Índices B-Tree compostos com import_id
CREATE INDEX IF NOT EXISTS idx_cards_import_oracle ON cards(import_id, oracle_id);
CREATE INDEX IF NOT EXISTS idx_cards_import_name ON cards(import_id, name);
CREATE INDEX IF NOT EXISTS idx_cards_import_cmc ON cards(import_id, cmc);
CREATE INDEX IF NOT EXISTS idx_card_faces_import_oracle ON card_faces(import_id, oracle_id);

-- 2. Índices GIN para Arrays e JSONB
CREATE INDEX IF NOT EXISTS idx_cards_colors_gin ON cards USING GIN(colors);
CREATE INDEX IF NOT EXISTS idx_cards_color_identity_gin ON cards USING GIN(color_identity);
CREATE INDEX IF NOT EXISTS idx_cards_keywords_gin ON cards USING GIN(keywords);
CREATE INDEX IF NOT EXISTS idx_cards_legalities_gin ON cards USING GIN(legalities);

-- 3. Índices GIN para busca aproximada por nome
CREATE INDEX IF NOT EXISTS idx_cards_name_trgm
ON cards USING GIN (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_card_faces_name_trgm
ON card_faces USING GIN (lower(name) gin_trgm_ops);

-- 4. Índice HNSW para Busca Vetorial
-- HNSW aceita no máximo 2.000 dimensões para vector e 4.000 para halfvec.
-- A coluna permanece em precisão completa; somente o índice usa meia precisão.
CREATE INDEX IF NOT EXISTS idx_cards_embedding_hnsw 
ON cards 
USING hnsw ((embedding::halfvec(1024)) halfvec_cosine_ops)
WITH (m = 16, ef_construction = 64);
