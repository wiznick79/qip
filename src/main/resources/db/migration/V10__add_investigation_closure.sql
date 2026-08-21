ALTER TABLE investigations
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN closure_summary VARCHAR(4000),
    ADD COLUMN closed_by VARCHAR(120),
    ADD COLUMN closed_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_investigations_status CHECK (status IN ('OPEN', 'CLOSED')),
    ADD CONSTRAINT chk_investigations_closure CHECK (
        (status = 'OPEN' AND closure_summary IS NULL AND closed_by IS NULL AND closed_at IS NULL)
        OR (status = 'CLOSED' AND closure_summary IS NOT NULL AND btrim(closure_summary) <> ''
            AND closed_by IS NOT NULL AND btrim(closed_by) <> '' AND closed_at IS NOT NULL)
    );
