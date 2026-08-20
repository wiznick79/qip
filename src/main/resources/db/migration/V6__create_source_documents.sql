CREATE TABLE source_documents (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(40) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(100) NOT NULL,
    ingestion_status VARCHAR(40) NOT NULL,
    failure_reason VARCHAR(500),
    uploaded_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_source_documents_checksum UNIQUE (checksum_sha256),
    CONSTRAINT uk_source_documents_storage_key UNIQUE (storage_key),
    CONSTRAINT chk_source_documents_title_nonblank CHECK (btrim(title) <> ''),
    CONSTRAINT chk_source_documents_filename_nonblank CHECK (btrim(original_filename) <> ''),
    CONSTRAINT chk_source_documents_media_type CHECK (media_type IN ('PDF', 'PLAIN_TEXT')),
    CONSTRAINT chk_source_documents_size_positive CHECK (size_bytes > 0),
    CONSTRAINT chk_source_documents_checksum_format CHECK (checksum_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_source_documents_status CHECK (
        ingestion_status IN ('UPLOADED', 'EXTRACTING', 'EXTRACTED', 'EXTRACTION_FAILED')
    ),
    CONSTRAINT chk_source_documents_failure_state CHECK (
        (ingestion_status = 'EXTRACTION_FAILED' AND failure_reason IS NOT NULL)
        OR (ingestion_status <> 'EXTRACTION_FAILED' AND failure_reason IS NULL)
    ),
    CONSTRAINT chk_source_documents_updated_after_upload CHECK (updated_at >= uploaded_at)
);

CREATE TABLE extracted_document_pages (
    document_id UUID NOT NULL REFERENCES source_documents(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    text TEXT NOT NULL,
    PRIMARY KEY (document_id, page_number),
    CONSTRAINT chk_extracted_pages_number_positive CHECK (page_number > 0),
    CONSTRAINT chk_extracted_pages_text_nonblank CHECK (btrim(text) <> '')
);
