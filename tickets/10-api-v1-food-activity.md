# 10 — API v1: Food & Activity

**Priority:** P2 — lower priority, rarely used by automated uploaders
**Depends on:** 04, 05
**Parallelizable with:** 06-09, 11

## Summary

Implement `/api/v1/food` and `/api/v1/activity` — simple CRUD endpoints for the food database and activity tracking.

## Endpoints

### Food: `GET/POST/PUT/DELETE /api/v1/food[.json]`

### Activity: `GET/POST/PUT/DELETE /api/v1/activity[.json]`

## Acceptance Criteria

- [ ] CRUD for both endpoints
- [ ] Query by date range, name
- [ ] Spring RestDocs tests
