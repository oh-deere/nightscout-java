# 09 — API v1: Device Status

**Priority:** P1
**Depends on:** 04, 05
**Parallelizable with:** 06, 07, 08, 10, 11

## Summary

Implement `/api/v1/devicestatus` — receives status reports from pumps, Loop, OpenAPS, AAPS, and uploaders. High volume (every 5 minutes from each device).

## Endpoints

### `GET /api/v1/devicestatus[.json]`

Query parameters: `count`, `find[created_at][$gte]`, `find[device]`

### `POST /api/v1/devicestatus`

Accepts single or array. Payloads are highly variable — the entire document is stored as `jsonb` with a few indexed columns extracted.

Example AAPS payload:
```json
{
  "device": "openaps://samsung SM-G991B",
  "created_at": "2024-01-15T10:30:00.000Z",
  "openaps": {
    "suggested": { "bg": 120, "tick": "+2", "eventualBG": 115, "IOB": 1.5 },
    "enacted": { "rate": 0.8, "duration": 30 },
    "iob": { "iob": 1.5, "basaliob": 0.3 }
  },
  "pump": {
    "battery": { "percent": 75 },
    "reservoir": 120.5,
    "status": { "status": "normal", "timestamp": "2024-01-15T10:29:00.000Z" }
  },
  "uploader": { "battery": 85 }
}
```

### `DELETE /api/v1/devicestatus/:id`

## Acceptance Criteria

- [ ] GET/POST/DELETE for devicestatus
- [ ] Full payload stored in `raw` jsonb column
- [ ] Key fields (`device`, `created_at`, `pump`, `openaps`, `loop`) extracted for indexed queries
- [ ] Handles variable payloads from Loop, OpenAPS, AAPS, xDrip+ without failing
- [ ] Spring RestDocs tests

## Notes

- This table grows fast. Consider a retention policy (e.g., delete devicestatus older than 90 days) — upstream Nightscout has no built-in retention.
- The pump/loop status plugin (ticket 20) reads from this data.
