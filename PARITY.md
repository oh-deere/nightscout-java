# Nightscout API parity report

Cross-check of our Java rewrite against upstream `nightscout/cgm-remote-monitor` v15.x. Goal:
identify what's missing before going live.

Legend: ✅ implemented · ⚠️ partial / minor gaps · ❌ missing · ⚪ not needed for our use case

## /api/v1 — Status, meta, auth probes

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v1/status` (`.json/.txt/.html`) | ✅ | We return JSON. `.txt` and `.html` not implemented; uploaders all use `.json`. |
| GET | `/api/v1/verifyauth` | ✅ | |
| GET | `/api/v1/experiments/test` | ⚪ | Debug endpoint, not used by uploaders. |
| GET | `/api/v1/adminnotifies` | ❌ | We removed our stub. Used by the legacy web UI; not needed by xDrip+/AAPS. |

## /api/v1 — Entries

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v1/entries` (+ `.json`) | ✅ | Supports `count`, `find[type]`, `find[date][$gte|$lte]`. Other operators not yet wired. |
| GET | `/api/v1/entries/current` | ✅ | |
| GET | `/api/v1/entries/sgv` | ✅ | |
| GET | `/api/v1/entries/:spec` | ❌ | "Get one by id or by dateString prefix". Needed by some clients to fetch by `_id`. |
| POST | `/api/v1/entries/preview` | ❌ | Validate without saving. Rarely used; AAPS doesn't. |
| POST | `/api/v1/entries` | ✅ | |
| DELETE | `/api/v1/entries/:id` | ✅ | UUID path |
| DELETE | `/api/v1/entries` (by query) | ❌ | Bulk delete by find filter. |
| Output formats | `.csv`, `.tsv`, `.txt`, `.svg`, `.png` | ❌ | Reports/legacy tools only — not needed by uploaders. |
| Slice/times/count/echo | `/slice/...`, `/times/...`, `/count/...`, `/echo/...` | ❌ | Advanced query helpers. Not used by xDrip+/AAPS/Loop. |

## /api/v1 — Treatments

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v1/treatments` | ✅ | Supports `count`, `find[eventType]`, `find[created_at][$gte]`. |
| POST | `/api/v1/treatments` | ✅ | Single or array. |
| PUT | `/api/v1/treatments` | ❌ | Update by `_id` in body. AAPS uses this for editing. |
| DELETE | `/api/v1/treatments/:id` | ✅ | |
| DELETE | `/api/v1/treatments` (by query) | ❌ | Bulk delete by find filter. |

## /api/v1 — Profile

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v1/profile` (+ `/profiles`) | ✅ (only `/profile`) | We don't expose the alias `/profiles`. AAPS hits `/profile`. |
| GET | `/api/v1/profile/current` | ❌ | Returns the current active profile. Used by AAPS. |
| POST | `/api/v1/profile` | ✅ | |
| PUT | `/api/v1/profile` | ❌ | Update by `_id`. |
| DELETE | `/api/v1/profile/:_id` | ✅ | |

## /api/v1 — Devicestatus

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v1/devicestatus` | ✅ | |
| POST | `/api/v1/devicestatus` | ✅ | |
| DELETE | `/api/v1/devicestatus/:id` | ✅ | |
| DELETE | `/api/v1/devicestatus` (by query) | ❌ | Bulk delete. Not critical. |

## /api/v1 — Food

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v1/food` | ✅ | |
| GET | `/api/v1/food/quickpicks` | ❌ | Subset filter. UI feature only. |
| GET | `/api/v1/food/regular` | ❌ | Subset filter. UI feature only. |
| POST | `/api/v1/food` | ✅ | |
| PUT | `/api/v1/food` | ❌ | Update by `_id`. |
| DELETE | `/api/v1/food/:_id` | ✅ | |

## /api/v1 — Activity

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v1/activity` | ❌ | We have a repository but no controller. |
| POST | `/api/v1/activity` | ❌ | |
| PUT | `/api/v1/activity` | ❌ | |
| DELETE | `/api/v1/activity/:_id` | ❌ | |

## /api/v1 — Notifications & properties

| Method | Path | Status | Notes |
|---|---|---|---|
| POST | `/api/v1/notifications/pushovercallback` | ⚪ | Pushover integration. We don't use it. |
| GET | `/api/v1/notifications/ack` | ⚠️ | We expose a `POST /notifications/ack`. Upstream is GET with query params. xDrip+ uses POST in newer versions; AAPS uses GET. |
| GET | `/api/v1/properties` (and `/api/v2/properties`) | ✅ (v1 only) | We expose the v1 path. Upstream lives at v2 too. |

## /api/v2 — Authorization & extras

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v2/authorization/request/:accessToken` | ❌ | Upstream returns the JWT for a given access token. **Critical for the official web client and many uploaders.** We have only the v3 variant at `/api/v3/authorization/request/{accessToken}` and a `POST /api/v2/authorization/request`. |
| POST | `/api/v2/authorization/request` | ✅ | Custom — not in upstream but we use it. |
| Subjects/roles management | `/api/v2/authorization/{subjects,roles,permissions,...}` | ❌ | Admin role/subject management. Not used by uploaders. |
| GET | `/api/v2/properties` | ❌ | Same data as v1; alias only. |
| GET | `/api/v2/ddata/at/:at?` | ❌ | Bulk dashboard snapshot. Used by some legacy clients. |
| GET | `/api/v2/summary` | ❌ | Snapshot. Not commonly used. |

## /api/v3 — Generic CRUD per collection

Upstream exposes the **same CRUD shape** for `entries`, `treatments`, `profile`, `devicestatus`, `food`, `settings`. We currently expose **only `entries`**.

Per collection:

| Method | Path | Entries | Treatments | Profile | Devicestatus | Food | Settings |
|---|---|---|---|---|---|---|---|
| GET | `/api/v3/<col>` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| POST | `/api/v3/<col>` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/api/v3/<col>/history` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/api/v3/<col>/history/:lastModified` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET | `/api/v3/<col>/:identifier` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PUT | `/api/v3/<col>/:identifier` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PATCH | `/api/v3/<col>/:identifier` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| DELETE | `/api/v3/<col>/:identifier` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

Plus query-param ops on the GET search (`<field>$eq`, `$gte`, `$lt`, `$in`, `$re`, `sort`, `sort$desc`, `fields`, `limit`, `skip`, `now`) — we support `date$gte`, `type$eq`, `limit`, `sort$desc` only.

## /api/v3 — Specific endpoints

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/api/v3/version` | ✅ | |
| GET | `/api/v3/status` | ✅ | |
| GET | `/api/v3/lastModified` | ✅ | Returns only `entries` timestamp; should also include treatments/profile/devicestatus/food/settings. |
| GET | `/api/v3/test` | ⚪ | Dev-only auth probe. |

## WebSocket / Socket.IO

| Namespace | Status | Notes |
|---|---|---|
| `/socket.io` (default ns) | ❌ | We removed the stub. Not needed by xDrip+/AAPS/Loop, but the legacy web UI requires it. |
| `/storage` (api3) | ❌ | Real-time CRUD events for v3 collections. Used by some sync clients. |
| `/alarm` (api3) | ❌ | Real-time alarm/announcement push. Used by the legacy web UI. |
| `/ws` (STOMP) | ✅ | Our own STOMP endpoint. Not used by any third-party clients. |

## Server / static routes

| Path | Status | Notes |
|---|---|---|
| `GET /api/versions` | ❌ | Lists supported API versions. Trivial to add. |
| `GET /sw.js` | ⚠️ | Served via React PWA bundle, not as a forced-content-type endpoint. |
| `GET /manifest.json` | ✅ | React app provides one. |
| `GET /translations/*` | ⚪ | Legacy i18n. Our React UI uses none. |
| `GET /pebble` | ⚪ | Pebble watch endpoint. Dead. |
| `GET /swagger.json`, `/swagger.yaml`, `/api-docs`, `/api3-docs` | ❌ | OpenAPI docs. We have RestDocs scaffolding but no published spec. |
| `POST /report-violation` | ⚪ | CSP violation sink. |
| Auth helper sub-apps (`/api/v1/alexa`, `/api/v1/googlehome`) | ⚪ | Voice assistants. Not needed. |

---

## Summary by client

### What xDrip+ uses (verified)
- `GET /api/v1/status.json` — ✅
- `POST /api/v1/entries` — ✅
- `POST /api/v1/treatments` — ✅
- `POST /api/v1/devicestatus` — ✅
- `GET /api/v1/profile` — ✅
- `GET /api/v1/entries/sgv.json` — ✅
- API-secret header auth — ✅

**xDrip+ should work today.**

### What AAPS uses
- All of the xDrip+ list above — ✅
- `GET /api/v1/profile/current` — ❌
- `PUT /api/v1/treatments` (edit existing) — ❌
- `DELETE /api/v1/treatments/:id` — ✅
- `GET /api/v2/authorization/request/:accessToken` — ❌
- `/api/v3/<col>/history/:lastModified` for sync — ❌

**AAPS will partially work** (uploads + reads). Editing treatments and v3 sync won't.

### What Loop (iOS) uses

After auditing LoopKit's `NightscoutClient.swift`, **Loop only uses v1 + one v2 endpoint** —
no v3 calls anywhere:

- `POST /api/v1/devicestatus` (heavy use) — ✅
- `POST /api/v1/treatments` — ✅ (preserves `syncIdentifier` and `insulinType`)
- `PUT /api/v1/treatments` — ✅
- `DELETE /api/v1/treatments/{_id}` — ✅
- `GET /api/v1/treatments?find[created_at][$gte|$lte]=...` — ✅
- `POST /api/v1/entries` — ✅
- `GET /api/v1/entries?find[dateString][$gte|$lte]=...` — ✅
- `POST /api/v1/profile`, `PUT /api/v1/profile` — ✅
- `GET /api/v1/profile/current` — ✅
- `GET /api/v1/profiles?find[startDate][$gte|$lte]=...` — ✅
- `GET /api/v1/experiments/test` (auth probe) — ✅
- `POST /api/v2/notifications/loop` (remote override / bolus / carbs) — ✅
- `api-secret` SHA-1 header auth — ✅
- HTTP 200 responses with array of `_id`-bearing objects — ✅

### What the **legacy** Nightscout web UI requires
- `/socket.io` namespace with authorize event — ❌
- `/api/v1/adminnotifies` — ❌
- Dozens of UI-only routes — ❌

We **don't need to support the legacy UI** since we built our own React frontend.

---

## Status

**Updated 2026-04-07**: All Tier-1 items + full Loop compatibility implemented and verified
by **45 Playwright e2e tests** against a running instance. xDrip+, AAPS, and LoopKit/Loop
uploaders should all work end-to-end.

## Critical gaps to close before go-live

These are the items I'd actually fix before deploying to k3s and pointing real CGM uploaders at it:

### Must-have (uploader compatibility) — ✅ DONE

1. ✅ **`PUT /api/v1/treatments`** — AAPS uses this to edit existing treatments
2. ✅ **`PUT /api/v1/profile`** — Profile updates from AAPS
3. ✅ **`GET /api/v1/profile/current`** — AAPS reads the active profile here
4. ✅ **`GET /api/v1/profiles`** alias — Defensive (some old clients hit it)
5. ✅ **`GET /api/v2/authorization/request/:accessToken`** — Token-based auth for uploaders that prefer JWT
6. ✅ **Activity controller (CRUD)** — Full CRUD wired
7. ✅ **`/api/v3/<col>` for treatments + devicestatus + profile** — Generic CRUD for all three collections

### Should-have (better client coverage) — ✅ DONE

8. ✅ **`GET /api/v3/<col>/history/:lastModified`** — Incremental sync for treatments / devicestatus / profile
9. ✅ **`/api/v3/lastModified` includes all collections** — entries + treatments + profile + devicestatus
10. ✅ **`GET /api/v1/entries/:spec`** — Fetch one entry by UUID
11. ✅ **`GET /api/versions`** — One-line endpoint
12. ✅ **`GET /api/v1/notifications/ack`** — GET form (AAPS) + POST form (xDrip+) both work

### Nice-to-have (deferred)

13. CSV/TSV output formats for entries (reports tools)
14. Bulk delete by query for entries/treatments/devicestatus
15. Subjects/roles admin endpoints (`/api/v2/authorization/...`)
16. Socket.IO `/storage` and `/alarm` namespaces (only the legacy UI cares)
17. OpenAPI / Swagger JSON publication

### Won't-do

- Legacy `/socket.io` default namespace — we have our own React UI
- `/translations`, `/pebble`, Alexa, Google Home, Pushover
- `.csv`/`.svg`/`.png` output formats (reports tools)
- Legacy web UI helper endpoints (`/api/v1/adminnotifies`)

---

## Loop (LoopKit) compatibility

After research into LoopKit/Loop's `NightscoutClient.swift` showed Loop uses only the v1
surface plus `POST /api/v2/notifications/loop`, the following changes were made for full
parity:

- ✅ **`/api/v1/experiments/test`** — Loop's auth probe before doing real work
- ✅ **`POST /api/v2/notifications/loop`** — Loop's remote command channel (override
  start/cancel, remote bolus, remote carbs). We persist the request as a treatment so it
  shows up in the treatment log; the APNs side-channel for actually triggering Loop is
  intentionally not implemented (single-user setup, Loop reads it back from the treatment
  stream).
- ✅ **`syncIdentifier` and `insulinType` columns** added to treatments via Flyway V2
  migration. Loop's stable client-side ids and insulin metadata are now persisted and
  round-tripped verbatim.
- ✅ **POST responses** are JSON arrays of `_id`-bearing objects with HTTP 200 (not 201/204).
  Loop treats anything else as a failure.
- ✅ **`find[startDate][$gte|$lte]`** filter on `/api/v1/profiles` for Loop's profile sync.
- ✅ **`find[dateString][$gte|$lte]`** filter on `/api/v1/entries` for Loop's entry sync.

11 Loop-specific e2e tests in `e2e/tests/loop-compatibility.spec.ts` lock these
behaviors in.
