CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL REFERENCES assets (id),
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000),
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT incidents_title_not_blank CHECK (BTRIM(title) <> ''),
    CONSTRAINT incidents_severity_valid CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT incidents_status_valid CHECK (status IN ('REPORTED', 'INVESTIGATING', 'RESOLVED', 'CLOSED')),
    CONSTRAINT incidents_updated_after_created CHECK (updated_at >= created_at)
);

CREATE INDEX incidents_occurred_id_idx ON incidents (occurred_at DESC, id);
CREATE INDEX incidents_asset_occurred_id_idx ON incidents (asset_id, occurred_at DESC, id);
CREATE INDEX incidents_status_occurred_id_idx ON incidents (status, occurred_at DESC, id);
