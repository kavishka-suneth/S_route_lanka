-- Add route activation state so route removal can be a safe soft delete.
ALTER TABLE routes
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE AFTER destination;

CREATE INDEX idx_routes_active ON routes (active);
