-- Nightscout Java — initial schema
-- Replaces MongoDB collections with PostgreSQL tables.

-- =============================================================================
-- Entries: CGM readings (SGV, MBG, calibrations)
-- Highest volume table — ~288 rows/day for 5-min CGM readings.
-- =============================================================================
CREATE TABLE entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type          TEXT NOT NULL,
    date_ms       BIGINT NOT NULL,
    date_string   TEXT,
    sys_time      TEXT,
    sgv           INTEGER,
    direction     TEXT,
    noise         INTEGER,
    filtered      DOUBLE PRECISION,
    unfiltered    DOUBLE PRECISION,
    rssi          INTEGER,
    device        TEXT,
    utc_offset    INTEGER DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (sys_time, type)
);

CREATE INDEX idx_entries_date ON entries (date_ms DESC);
CREATE INDEX idx_entries_type_date ON entries (type, date_ms DESC);

-- =============================================================================
-- Treatments: insulin, carbs, temp basals, profile switches, site changes, etc.
-- Polymorphic — 30+ event types. Common fields as columns, rest in jsonb.
-- =============================================================================
CREATE TABLE treatments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      TEXT NOT NULL,
    created_at_str  TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    entered_by      TEXT,
    notes           TEXT,
    insulin         DOUBLE PRECISION,
    carbs           DOUBLE PRECISION,
    glucose         DOUBLE PRECISION,
    glucose_type    TEXT,
    duration        DOUBLE PRECISION,
    utc_offset      INTEGER DEFAULT 0,
    details         JSONB DEFAULT '{}',
    UNIQUE (event_type, created_at_str)
);

CREATE INDEX idx_treatments_created ON treatments (created_at DESC);
CREATE INDEX idx_treatments_event_type ON treatments (event_type, created_at DESC);

-- =============================================================================
-- Device status: pump, loop, OpenAPS, uploader status reports.
-- Highly variable structure — store full payload as jsonb.
-- =============================================================================
CREATE TABLE device_status (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    device      TEXT,
    uploader    JSONB,
    pump        JSONB,
    openaps     JSONB,
    loop        JSONB,
    xdripjs     JSONB,
    raw         JSONB NOT NULL
);

CREATE INDEX idx_devicestatus_created ON device_status (created_at DESC);

-- =============================================================================
-- Profiles: basal rates, ISF, IC ratios, target ranges.
-- =============================================================================
CREATE TABLE profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    default_profile TEXT,
    store           JSONB NOT NULL
);

CREATE INDEX idx_profiles_created ON profiles (created_at DESC);

-- =============================================================================
-- Food database for carb counting.
-- =============================================================================
CREATE TABLE food (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    name        TEXT,
    category    TEXT,
    portion     DOUBLE PRECISION,
    unit        TEXT,
    carbs       DOUBLE PRECISION,
    fat         DOUBLE PRECISION,
    protein     DOUBLE PRECISION,
    details     JSONB DEFAULT '{}'
);

-- =============================================================================
-- Activity / exercise tracking.
-- =============================================================================
CREATE TABLE activity (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    name        TEXT,
    duration    DOUBLE PRECISION,
    details     JSONB DEFAULT '{}'
);

-- =============================================================================
-- API keys for Nightscout-native auth (API_SECRET + per-subject tokens).
-- =============================================================================
CREATE TABLE api_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    key_hash    TEXT NOT NULL UNIQUE,
    permissions TEXT[],
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    enabled     BOOLEAN DEFAULT true
);

-- =============================================================================
-- Settings: per-app settings storage (API v3).
-- =============================================================================
CREATE TABLE settings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier   TEXT NOT NULL UNIQUE,
    data         JSONB NOT NULL,
    srv_modified BIGINT NOT NULL,
    is_valid     BOOLEAN DEFAULT true
);
