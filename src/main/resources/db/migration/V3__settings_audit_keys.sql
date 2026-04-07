-- Runtime-editable settings, scoped API keys, and an audit trail.
--
-- The bootstrap API_SECRET in the env var remains the admin path. Per-app keys live in
-- this table and are addressed via the ?token=<plaintext> query parameter (matches
-- upstream Nightscout's Subject/access-token model).

-- Drop the placeholder api_keys table from V1 (no rows expected; was never wired).
DROP TABLE IF EXISTS api_keys;

CREATE TABLE api_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    token_hash  TEXT NOT NULL UNIQUE,         -- SHA-256 hex of the plaintext token
    scope       TEXT NOT NULL,                -- 'read', 'write', or 'admin'
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  TEXT NOT NULL,
    last_used_at TIMESTAMPTZ NULL,
    expires_at  TIMESTAMPTZ NULL,
    enabled     BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT api_keys_scope_valid CHECK (scope IN ('read', 'write', 'admin'))
);

CREATE INDEX idx_api_keys_token_hash ON api_keys (token_hash);
CREATE INDEX idx_api_keys_enabled ON api_keys (enabled) WHERE enabled = true;

-- Runtime settings: one row per dotted key, value stored as JSONB so we can hold
-- numbers, strings, booleans, or nested objects without per-type columns.
CREATE TABLE runtime_settings (
    key         TEXT PRIMARY KEY,
    value       JSONB NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  TEXT NOT NULL
);

-- Audit log: every write to settings or api_keys creates an entry. Reads are not
-- audited.
CREATE TABLE audit_log (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_subject TEXT NOT NULL,             -- "admin" (bootstrap) or api key name
    actor_kind    TEXT NOT NULL,             -- 'bootstrap-secret', 'api-key', 'oauth', 'system'
    action        TEXT NOT NULL,             -- e.g. 'settings.update', 'key.create', 'key.revoke'
    target        TEXT NOT NULL,             -- the setting key, api-key name, etc.
    before_value  JSONB NULL,
    after_value   JSONB NULL
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_log_target ON audit_log (target);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_subject, occurred_at DESC);
