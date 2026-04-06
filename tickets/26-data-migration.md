# 26 — Data Migration Tool

**Priority:** P1 — needed before go-live
**Depends on:** 02 (schema), 04 (storage layer)
**Parallelizable with:** all feature tickets

## Summary

One-shot tool to migrate existing data from a MongoDB-backed Nightscout instance to the new PostgreSQL schema.

## Approach

Build a CLI command (Spring Boot `CommandLineRunner` with a profile) that:

1. Connects to the source MongoDB (via connection string)
2. Reads all documents from each collection
3. Transforms to the PostgreSQL schema
4. Batch-inserts into PostgreSQL

### Collections to Migrate

| Collection | Expected Volume | Notes |
|-----------|----------------|-------|
| entries | ~100K+ per year | Highest volume, 288/day |
| treatments | ~5-50K per year | Variable, depends on therapy |
| devicestatus | ~100K+ per year | High volume, consider retention cutoff |
| profile | <100 total | Small |
| food | <1000 | Small |
| activity | <1000 | Small |

### Transformation

- MongoDB `_id` (ObjectID) → generate new UUID, store original `_id` in a `legacy_id` text column for reference
- `date` (epoch ms) → keep as `date_ms`, also parse to `created_at` timestamp
- Treatments: extract common fields into columns, remainder into `details` jsonb
- Devicestatus: extract `device`, `created_at`, store full document as `raw` jsonb

## Configuration

```
MIGRATION_MONGO_URI=mongodb://host:27017/nightscout
MIGRATION_BATCH_SIZE=1000
MIGRATION_DEVICESTATUS_CUTOFF_DAYS=90
```

## Acceptance Criteria

- [ ] Reads from MongoDB, writes to PostgreSQL
- [ ] Batch processing (not one-by-one) for performance
- [ ] Progress logging (X of Y entries migrated)
- [ ] Handles duplicates gracefully (ON CONFLICT DO NOTHING)
- [ ] Optional devicestatus retention cutoff
- [ ] Idempotent — can be re-run safely
- [ ] Integration test with embedded MongoDB + Testcontainers PostgreSQL

## Notes

- This is a one-shot migration, not ongoing sync. After migration, the MongoDB instance can be decommissioned.
- Consider running against a MongoDB dump/export rather than a live instance if downtime is a concern.
