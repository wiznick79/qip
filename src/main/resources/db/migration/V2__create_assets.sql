CREATE TABLE assets (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    asset_type VARCHAR(40) NOT NULL,
    external_reference VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT assets_name_not_blank CHECK (BTRIM(name) <> '')
);

CREATE INDEX assets_name_id_idx ON assets (name, id);
