CREATE TABLE incident_observations (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents (id),
    observation_text VARCHAR(4000) NOT NULL,
    author_reference VARCHAR(120) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT incident_observations_text_not_blank CHECK (BTRIM(observation_text) <> ''),
    CONSTRAINT incident_observations_author_not_blank CHECK (BTRIM(author_reference) <> ''),
    CONSTRAINT incident_observations_time_valid CHECK (observed_at <= recorded_at)
);

CREATE INDEX incident_observations_timeline_idx
    ON incident_observations (incident_id, observed_at, id);
