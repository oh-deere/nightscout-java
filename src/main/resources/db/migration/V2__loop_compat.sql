-- Loop compatibility additions:
-- 1. syncIdentifier on treatments — Loop's stable client-side id, must round-trip verbatim
ALTER TABLE treatments ADD COLUMN sync_identifier TEXT;
ALTER TABLE treatments ADD COLUMN insulin_type TEXT;

CREATE INDEX idx_treatments_sync_identifier ON treatments (sync_identifier)
    WHERE sync_identifier IS NOT NULL;
