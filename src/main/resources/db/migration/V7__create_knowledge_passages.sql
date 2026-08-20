ALTER TABLE source_documents DROP CONSTRAINT chk_source_documents_status;
ALTER TABLE source_documents DROP CONSTRAINT chk_source_documents_failure_state;

ALTER TABLE source_documents ADD CONSTRAINT chk_source_documents_status CHECK (
    ingestion_status IN (
        'UPLOADED', 'EXTRACTING', 'EXTRACTED', 'EXTRACTION_FAILED',
        'INDEXING', 'INDEXED', 'INDEXING_FAILED'
    )
);
ALTER TABLE source_documents ADD CONSTRAINT chk_source_documents_failure_state CHECK (
    (ingestion_status IN ('EXTRACTION_FAILED', 'INDEXING_FAILED') AND failure_reason IS NOT NULL)
    OR (ingestion_status NOT IN ('EXTRACTION_FAILED', 'INDEXING_FAILED') AND failure_reason IS NULL)
);

CREATE TABLE knowledge_passages (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES source_documents(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    page_number INTEGER NOT NULL,
    text TEXT NOT NULL,
    text_sha256 VARCHAR(64) NOT NULL,
    embedding VECTOR NOT NULL,
    embedding_model VARCHAR(120) NOT NULL,
    embedding_dimensions INTEGER NOT NULL,
    indexed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_knowledge_passages_document_sequence UNIQUE (document_id, sequence_number),
    CONSTRAINT chk_knowledge_passages_sequence_nonnegative CHECK (sequence_number >= 0),
    CONSTRAINT chk_knowledge_passages_page_positive CHECK (page_number > 0),
    CONSTRAINT chk_knowledge_passages_text_nonblank CHECK (btrim(text) <> ''),
    CONSTRAINT chk_knowledge_passages_hash_format CHECK (text_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_knowledge_passages_model_nonblank CHECK (btrim(embedding_model) <> ''),
    CONSTRAINT chk_knowledge_passages_dimensions_positive CHECK (embedding_dimensions > 0)
);

CREATE INDEX knowledge_passages_document_sequence_idx
    ON knowledge_passages (document_id, sequence_number);
