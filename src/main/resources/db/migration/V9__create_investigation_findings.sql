CREATE TABLE investigation_findings (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES investigations(id) ON DELETE CASCADE,
    source_question_id UUID NOT NULL REFERENCES investigation_questions(id),
    summary VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    proposed_by VARCHAR(120) NOT NULL,
    proposed_at TIMESTAMPTZ NOT NULL,
    reviewed_by VARCHAR(120),
    review_rationale VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT uk_investigation_findings_source_question UNIQUE (source_question_id),
    CONSTRAINT chk_investigation_findings_summary_nonblank CHECK (btrim(summary) <> ''),
    CONSTRAINT chk_investigation_findings_proposed_by_nonblank CHECK (btrim(proposed_by) <> ''),
    CONSTRAINT chk_investigation_findings_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT chk_investigation_findings_review CHECK (
        (status = 'DRAFT' AND reviewed_by IS NULL AND review_rationale IS NULL AND reviewed_at IS NULL)
        OR (status IN ('CONFIRMED', 'REJECTED') AND reviewed_by IS NOT NULL
            AND btrim(reviewed_by) <> '' AND review_rationale IS NOT NULL
            AND btrim(review_rationale) <> '' AND reviewed_at IS NOT NULL)
    )
);

CREATE INDEX investigation_findings_timeline_idx
    ON investigation_findings (investigation_id, proposed_at, id);

CREATE TABLE finding_review_events (
    id UUID PRIMARY KEY,
    finding_id UUID NOT NULL REFERENCES investigation_findings(id) ON DELETE CASCADE,
    event_type VARCHAR(16) NOT NULL,
    actor_reference VARCHAR(120) NOT NULL,
    rationale VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_finding_review_events_type CHECK (event_type IN ('PROPOSED', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT chk_finding_review_events_actor_nonblank CHECK (btrim(actor_reference) <> ''),
    CONSTRAINT chk_finding_review_events_rationale CHECK (
        (event_type = 'PROPOSED' AND rationale IS NULL)
        OR (event_type IN ('CONFIRMED', 'REJECTED') AND rationale IS NOT NULL AND btrim(rationale) <> '')
    )
);

CREATE INDEX finding_review_events_timeline_idx
    ON finding_review_events (finding_id, occurred_at, id);
