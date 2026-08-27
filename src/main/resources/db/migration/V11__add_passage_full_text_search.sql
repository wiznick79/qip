ALTER TABLE knowledge_passages
    ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('english', text)) STORED;

CREATE INDEX knowledge_passages_search_vector_idx
    ON knowledge_passages USING GIN (search_vector);
