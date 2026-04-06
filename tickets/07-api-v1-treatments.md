# 07 — API v1: Treatments

**Priority:** P0 — used by AAPS, Loop, and careportal
**Depends on:** 04, 05
**Parallelizable with:** 06, 08, 09, 10, 11

## Summary

Implement `/api/v1/treatments` — stores insulin boluses, carb entries, temp basals, profile switches, site changes, and 25+ other event types.

## Endpoints

### `GET /api/v1/treatments[.json]`

Query parameters:
- `count` — max results (default 10)
- `find[eventType]` — filter by event type
- `find[created_at][$gte]` — date range
- `find[insulin][$gt]=0` — only insulin treatments
- `find[carbs][$gt]=0` — only carb treatments

### `POST /api/v1/treatments`

Accepts single treatment or array. Deduplication by eventType + created_at.

### `PUT /api/v1/treatments`

Update existing treatment.

### `DELETE /api/v1/treatments/:id`

Delete a treatment (requires `api:treatments:delete`).

## Event Types

The most common event types AAPS/Loop send:

| Event Type | Key Fields |
|-----------|------------|
| Temp Basal | `duration`, `percent` or `absolute` |
| Correction Bolus | `insulin` |
| Meal Bolus | `insulin`, `carbs` |
| Carb Correction | `carbs` |
| Site Change | `notes` |
| Sensor Start | `notes` |
| Profile Switch | `profile`, `duration` |
| Announcement | `notes`, `isAnnouncement` |
| Note | `notes` |
| BG Check | `glucose`, `glucoseType` |
| OpenAPS Offline | `duration`, `reason` |

Fields not in the common columns go into the `details` jsonb column.

## Acceptance Criteria

- [ ] CRUD endpoints for treatments
- [ ] All 30+ event types handled (common fields extracted, rest in jsonb)
- [ ] Deduplication by eventType + created_at
- [ ] Query filters: eventType, date range, insulin, carbs
- [ ] Auth: write requires `api:treatments:create`
- [ ] Spring RestDocs tests
- [ ] Integration test with captured AAPS treatment payloads
