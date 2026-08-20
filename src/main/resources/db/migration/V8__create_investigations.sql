CREATE TABLE investigations (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_investigations_incident UNIQUE (incident_id),
    CONSTRAINT chk_investigations_updated_after_created CHECK (updated_at >= created_at)
);

CREATE TABLE investigation_questions (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES investigations(id) ON DELETE CASCADE,
    question_text VARCHAR(1000) NOT NULL,
    selected_document_ids UUID[] NOT NULL DEFAULT '{}',
    answer_status VARCHAR(32) NOT NULL,
    answer_text TEXT,
    model_id VARCHAR(120),
    prompt_version VARCHAR(40) NOT NULL,
    retrieved_passage_count INTEGER NOT NULL DEFAULT 0,
    failure_reason VARCHAR(500),
    asked_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_investigation_questions_text_nonblank CHECK (btrim(question_text) <> ''),
    CONSTRAINT chk_investigation_questions_status CHECK (
        answer_status IN ('PROCESSING', 'GROUNDED', 'INSUFFICIENT_EVIDENCE', 'TECHNICAL_FAILURE')
    ),
    CONSTRAINT chk_investigation_questions_retrieved_nonnegative CHECK (retrieved_passage_count >= 0),
    CONSTRAINT chk_investigation_questions_completion CHECK (
        (answer_status = 'PROCESSING' AND answer_text IS NULL AND model_id IS NULL
            AND failure_reason IS NULL AND completed_at IS NULL)
        OR (answer_status = 'GROUNDED' AND answer_text IS NOT NULL AND model_id IS NOT NULL
            AND failure_reason IS NULL AND completed_at IS NOT NULL)
        OR (answer_status = 'INSUFFICIENT_EVIDENCE' AND answer_text IS NOT NULL
            AND failure_reason IS NULL AND completed_at IS NOT NULL)
        OR (answer_status = 'TECHNICAL_FAILURE' AND answer_text IS NULL
            AND failure_reason IS NOT NULL AND completed_at IS NOT NULL)
    )
);

CREATE INDEX investigation_questions_timeline_idx
    ON investigation_questions (investigation_id, asked_at, id);

CREATE TABLE answer_citations (
    question_id UUID NOT NULL REFERENCES investigation_questions(id) ON DELETE CASCADE,
    ordinal INTEGER NOT NULL,
    passage_id UUID NOT NULL,
    document_id UUID NOT NULL,
    document_title VARCHAR(200) NOT NULL,
    page_number INTEGER NOT NULL,
    passage_sequence INTEGER NOT NULL,
    excerpt VARCHAR(500) NOT NULL,
    relevance_score DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (question_id, ordinal),
    CONSTRAINT chk_answer_citations_ordinal_nonnegative CHECK (ordinal >= 0),
    CONSTRAINT chk_answer_citations_page_positive CHECK (page_number > 0),
    CONSTRAINT chk_answer_citations_sequence_nonnegative CHECK (passage_sequence >= 0),
    CONSTRAINT chk_answer_citations_title_nonblank CHECK (btrim(document_title) <> ''),
    CONSTRAINT chk_answer_citations_excerpt_nonblank CHECK (btrim(excerpt) <> '')
);
