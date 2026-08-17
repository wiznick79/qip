CREATE TABLE incident_evidence (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents (id),
    evidence_type VARCHAR(30) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    source_reference VARCHAR(500) NOT NULL,
    event_at TIMESTAMPTZ NOT NULL,
    provenance VARCHAR(30) NOT NULL,
    submitted_by VARCHAR(120) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT incident_evidence_type_valid CHECK (
        evidence_type IN ('MEASUREMENT', 'DOCUMENT', 'IMAGE', 'LOG_ENTRY', 'PHYSICAL_ITEM', 'TEST_RESULT', 'OTHER')
    ),
    CONSTRAINT incident_evidence_summary_not_blank CHECK (BTRIM(summary) <> ''),
    CONSTRAINT incident_evidence_source_not_blank CHECK (BTRIM(source_reference) <> ''),
    CONSTRAINT incident_evidence_provenance_valid CHECK (
        provenance IN ('HUMAN_ENTERED', 'IMPORTED', 'RETRIEVED', 'MODEL_GENERATED')
    ),
    CONSTRAINT incident_evidence_submitter_not_blank CHECK (BTRIM(submitted_by) <> ''),
    CONSTRAINT incident_evidence_time_valid CHECK (event_at <= recorded_at)
);

CREATE INDEX incident_evidence_timeline_idx
    ON incident_evidence (incident_id, event_at, id);
