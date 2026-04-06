# 06 — API v1: Entries

**Priority:** P0 — core data endpoint, used by all uploaders
**Depends on:** 04, 05
**Parallelizable with:** 07, 08, 09, 10, 11

## Summary

Implement the `/api/v1/entries` endpoint — the most critical endpoint in Nightscout. Every CGM uploader (xDrip+, AAPS, Loop, Dexcom bridge) pushes SGV data here.

## Endpoints

### `GET /api/v1/entries[.json]`

Returns entries matching query filters. Default: latest 10 SGV entries.

Query parameters:
- `count` — max results (default 10)
- `find[type]` — filter by type (sgv, mbg, cal)
- `find[date][$gte]` — date range start (epoch ms)
- `find[date][$lte]` — date range end (epoch ms)
- `find[sgv][$gte]` / `[$lte]` — SGV value range
- `find[dateString][$gte]` — ISO date range

### `GET /api/v1/entries/current.json`

Returns the most recent SGV entry.

### `GET /api/v1/entries/sgv.json`

Returns latest entries filtered to type=sgv.

### `POST /api/v1/entries`

Accepts a single entry or array of entries. Must deduplicate (upsert by sysTime + type).

Request body:
```json
[{
  "type": "sgv",
  "dateString": "2024-01-15T10:30:00.000Z",
  "date": 1705312200000,
  "sgv": 120,
  "direction": "Flat",
  "device": "xDrip-DexcomG6",
  "filtered": 0,
  "unfiltered": 0,
  "noise": 1,
  "rssi": 100,
  "sysTime": "2024-01-15T10:30:00.000Z"
}]
```

### `GET /api/v1/entries/slice/:storage/:field/:type/:prefix/:regex`

Slice query — advanced filtering used by some clients.

### `DELETE /api/v1/entries/:id`

Delete a single entry (requires admin or `api:entries:delete` permission).

## Response Format

Nightscout returns entries with `_id` as a string (was MongoDB ObjectID). We return UUID as string in the `_id` field for compatibility.

```json
{
  "_id": "a1b2c3d4-...",
  "type": "sgv",
  "sgv": 120,
  "direction": "Flat",
  "date": 1705312200000,
  "dateString": "2024-01-15T10:30:00.000Z",
  "sysTime": "2024-01-15T10:30:00.000Z",
  "device": "xDrip-DexcomG6",
  "utcOffset": 0,
  "noise": 1
}
```

## Acceptance Criteria

- [ ] `GET /api/v1/entries.json` returns latest entries with correct format
- [ ] `GET /api/v1/entries/current.json` returns single latest SGV
- [ ] `POST /api/v1/entries` accepts single and array payloads
- [ ] Deduplication works (duplicate sysTime+type does upsert, not duplicate insert)
- [ ] Query filters work: count, date range, sgv range, type filter
- [ ] `_id` field returned as string for backwards compatibility
- [ ] Auth: POST requires `api:entries:create`, GET requires `api:entries:read`
- [ ] Spring RestDocs tests for all endpoints
- [ ] Integration test with real xDrip+ payload captured from production

## Notes

- This is the #1 endpoint to get right. Test with real payloads from xDrip+ and AAPS.
- Some uploaders send entries with extra fields not in our schema — store these in a `jsonb` overflow column or ignore gracefully.
- The `.json` suffix is optional but must be supported (some clients hardcode it).
