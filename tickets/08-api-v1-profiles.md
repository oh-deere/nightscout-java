# 08 — API v1: Profiles

**Priority:** P1
**Depends on:** 04, 05
**Parallelizable with:** 06, 07, 09, 10, 11

## Summary

Implement `/api/v1/profile` — stores basal rate profiles, insulin sensitivity factors (ISF), insulin-to-carb ratios (IC), and target BG ranges. Used by IOB/COB plugins and Loop/AAPS.

## Endpoints

### `GET /api/v1/profile[.json]`

Returns all profiles (or latest). Profiles contain a `store` object with named profiles.

### `POST /api/v1/profile`

Create or update a profile. The `store` field is a nested JSON object:

```json
{
  "defaultProfile": "Default",
  "store": {
    "Default": {
      "dia": 4,
      "carbratio": [{"time": "00:00", "value": 10}],
      "sens": [{"time": "00:00", "value": 50}],
      "basal": [{"time": "00:00", "value": 0.8}, {"time": "06:00", "value": 1.0}],
      "target_low": [{"time": "00:00", "value": 80}],
      "target_high": [{"time": "00:00", "value": 120}],
      "units": "mg/dl",
      "timezone": "Europe/Stockholm"
    }
  }
}
```

### `DELETE /api/v1/profile/:id`

## Acceptance Criteria

- [ ] GET/POST/DELETE for profiles
- [ ] `store` field stored as jsonb and returned verbatim
- [ ] Profile data accessible to IOB/COB plugins (ticket 17/18)
- [ ] Spring RestDocs tests
