# 02 — Database Schema & Migrations

**Priority:** P0 — blocks all storage/API work
**Depends on:** 01
**Parallelizable with:** 03 (domain model)

## Summary

Design the PostgreSQL schema and write Flyway migrations for all Nightscout data.

## Key Design Decisions

### Entries table (highest volume — ~288 rows/day for 5-min CGM readings)

The `entries` collection in MongoDB stores SGV readings, MBG (meter) readings, and calibrations.
These have a regular, predictable schema — use proper columns.

```sql
CREATE TABLE entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type          TEXT NOT NULL,           -- 'sgv', 'mbg', 'cal'
    date_ms       BIGINT NOT NULL,         -- epoch millis (Nightscout convention)
    date_string   TEXT,                    -- ISO 8601 string
    sys_time      TEXT,                    -- system time from uploader
    sgv           INTEGER,                 -- glucose value (mg/dL) for type=sgv
    direction     TEXT,                    -- trend arrow: Flat, FortyFiveUp, etc.
    noise         INTEGER,
    filtered      DOUBLE PRECISION,
    unfiltered    DOUBLE PRECISION,
    rssi          INTEGER,
    device        TEXT,
    utc_offset    INTEGER DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (sys_time, type)               -- deduplication key
);

CREATE INDEX idx_entries_date ON entries (date_ms DESC);
CREATE INDEX idx_entries_type_date ON entries (type, date_ms DESC);
```

### Treatments table (polymorphic — 30+ event types)

Use a combination of common columns + `jsonb` for event-type-specific data.

```sql
CREATE TABLE treatments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      TEXT NOT NULL,           -- 'Temp Basal', 'Correction Bolus', 'Site Change', etc.
    created_at_str  TEXT NOT NULL,           -- ISO string (Nightscout convention, used for queries)
    created_at      TIMESTAMPTZ NOT NULL,    -- parsed timestamp for proper indexing
    entered_by      TEXT,
    notes           TEXT,
    insulin         DOUBLE PRECISION,
    carbs           DOUBLE PRECISION,
    glucose         DOUBLE PRECISION,
    glucose_type    TEXT,
    duration        DOUBLE PRECISION,        -- minutes
    utc_offset      INTEGER DEFAULT 0,
    details         JSONB DEFAULT '{}',      -- event-type-specific fields
    UNIQUE (event_type, created_at_str)      -- deduplication key
);

CREATE INDEX idx_treatments_created ON treatments (created_at DESC);
CREATE INDEX idx_treatments_event_type ON treatments (event_type, created_at DESC);
```

### Devicestatus table (deeply nested, variable structure)

Device status reports are highly variable (Loop, OpenAPS, pump, uploader).
Store the entire payload as `jsonb` with a few indexed columns.

```sql
CREATE TABLE device_status (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    device      TEXT,
    uploader    JSONB,
    pump        JSONB,
    openaps     JSONB,
    loop        JSONB,
    xdripjs     JSONB,
    raw         JSONB NOT NULL             -- full original payload
);

CREATE INDEX idx_devicestatus_created ON device_status (created_at DESC);
```

### Profiles table

```sql
CREATE TABLE profiles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    default_profile TEXT,
    store       JSONB NOT NULL             -- named profiles with basal, ISF, IC, targets
);

CREATE INDEX idx_profiles_created ON profiles (created_at DESC);
```

### Food & Activity tables

```sql
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

CREATE TABLE activity (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    name        TEXT,
    duration    DOUBLE PRECISION,
    details     JSONB DEFAULT '{}'
);
```

### API keys & settings

```sql
CREATE TABLE api_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    key_hash    TEXT NOT NULL UNIQUE,       -- SHA-1 hash of API_SECRET
    permissions TEXT[],                      -- shiro-style permission strings
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    enabled     BOOLEAN DEFAULT true
);

CREATE TABLE settings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier   TEXT NOT NULL UNIQUE,
    data         JSONB NOT NULL,
    srv_modified BIGINT NOT NULL,
    is_valid     BOOLEAN DEFAULT true
);
```

## Acceptance Criteria

- [ ] `V1__init.sql` creates all tables, indexes, and constraints
- [ ] Schema supports the Nightscout deduplication patterns (entries, treatments)
- [ ] `jsonb` used for polymorphic/nested data (treatments.details, devicestatus, profiles.store)
- [ ] Proper columns for high-query fields (date_ms, type, sgv, event_type, created_at)
- [ ] Application starts with Flyway running migrations against Testcontainers Postgres
- [ ] Document any schema decisions that diverge from MongoDB structure

## Notes

- MongoDB `ObjectID` is replaced with UUID. The API layer must handle `_id` field mapping for backwards compatibility.
- Nightscout clients send `date` as epoch millis and `dateString` as ISO — keep both for compatibility.
- The `UNIQUE` constraints replace MongoDB upsert-based deduplication.
