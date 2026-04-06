# 13 — API v3: Generic CRUD

**Priority:** P1 — used by newer Nightscout clients
**Depends on:** 04, 05
**Parallelizable with:** 12, 14, 15

## Summary

Implement the Nightscout API v3 generic collection endpoints. API v3 is a cleaner REST API with JWT auth and a unified pattern for all collections.

## Endpoints

### `GET /api/v3/{collection}`

Generic list with query parameters:
- `sort` / `sort$desc` — field to sort by
- `limit` — max results (default 10, max 1000)
- `skip` — offset for pagination
- `fields` — comma-separated field list to return
- `{field}$eq`, `{field}$gte`, `{field}$lte`, `{field}$gt`, `{field}$lt`, `{field}$ne` — field filters
- `date$gte`, `date$lte` — date range (most common filter)

### `GET /api/v3/{collection}/{identifier}`

Get single document by identifier.

### `POST /api/v3/{collection}`

Create document. Returns `201` with `identifier` and `isDeduplication` flag.

### `PUT /api/v3/{collection}/{identifier}`

Full replace.

### `PATCH /api/v3/{collection}/{identifier}`

Partial update.

### `DELETE /api/v3/{collection}/{identifier}`

Soft delete (sets `isValid=false`).

### `GET /api/v3/lastModified`

Returns last modification timestamps per collection.

### `GET /api/v3/version`

Returns `{"version":"3.0.0","apiVersion":"3.0.0"}`.

### `GET /api/v3/status`

Server status (similar to v1 but different format).

## Collections

`entries`, `treatments`, `profile`, `devicestatus`, `food`, `activity`, `settings`

## Acceptance Criteria

- [ ] Generic controller that routes `{collection}` to the correct repository
- [ ] All CRUD operations for all collections
- [ ] Query parameter parsing (field filters, sort, limit, skip)
- [ ] `lastModified` endpoint returns per-collection timestamps
- [ ] Soft delete via `isValid` flag
- [ ] `identifier` field support (API v3 uses `identifier` instead of `_id`)
- [ ] Spring RestDocs tests

## Notes

- API v3 uses `identifier` (a string, often the sysTime or a generated value) instead of MongoDB `_id`. Map to a dedicated column or use UUID string.
- The `srvModified` field tracks server-side modification time for sync — add a `srv_modified` column or use `updated_at`.
