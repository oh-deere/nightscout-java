# nightscout-java — Migration Tickets

Full rewrite of [Nightscout cgm-remote-monitor](https://github.com/nightscout/cgm-remote-monitor) (v15.0.6)
from Node.js + MongoDB to Java 25 / Spring Boot 4 / PostgreSQL,
following OhDeere conventions.

## Goal

Replace the upstream Nightscout server with a Spring Boot service that:

1. Pulls CGM data from LibreLink Up (FreeStyle Libre) via built-in bridge.
2. Implements the Nightscout API v1 contract so existing apps (xDrip+, AAPS, Loop) can read data.
3. Implements the Nightscout API v3 contract for modern clients.
4. Stores all data in PostgreSQL (CloudNativePG cluster).
5. Supports real-time WebSocket push via Socket.IO / STOMP.
6. Ports the core medical algorithms (IOB, COB, AR2 prediction).
7. Runs on the OhDeere k3s cluster alongside existing services.
8. Eventually replaces the legacy jQuery/D3 frontend with a React + TypeScript SPA.

## Ticket Order

Tickets are numbered for suggested implementation order. Some can be parallelized (noted in each ticket).

| # | Ticket | Summary |
|---|--------|---------|
| 01 | [Project scaffold](01-project-scaffold.md) | Maven project, Spring Boot 4, CI/CD |
| 02 | [Database schema & migrations](02-database-schema.md) | Flyway migrations for all core tables |
| 03 | [Domain model](03-domain-model.md) | Java records for entries, treatments, profiles, devicestatus, food, activity |
| 04 | [Storage layer](04-storage-layer.md) | Spring Data JDBC repositories |
| 05 | [Auth system](05-auth-system.md) | API key hashing, JWT, permission model |
| 06 | [API v1 — entries](06-api-v1-entries.md) | GET/POST /api/v1/entries (SGV, MBG, calibrations) |
| 07 | [API v1 — treatments](07-api-v1-treatments.md) | GET/POST /api/v1/treatments |
| 08 | [API v1 — profiles](08-api-v1-profiles.md) | GET/POST /api/v1/profile |
| 09 | [API v1 — devicestatus](09-api-v1-devicestatus.md) | GET/POST /api/v1/devicestatus |
| 10 | [API v1 — food & activity](10-api-v1-food-activity.md) | GET/POST /api/v1/food, /api/v1/activity |
| 11 | [API v1 — status & misc](11-api-v1-status.md) | /api/v1/status, /verifyauth, /notifications |
| 12 | [API v1 — query & output formats](12-api-v1-query-formats.md) | Query parsing, CSV/JSON/SVG output |
| 13 | [API v3 — generic CRUD](13-api-v3-crud.md) | /api/v3/{collection} generic endpoints |
| 14 | [API v3 — auth & permissions](14-api-v3-auth.md) | JWT subjects, role-based access, token lifecycle |
| 15 | [WebSocket — real-time data push](15-websocket.md) | Socket.IO-compatible or STOMP for live SGV updates |
| 16 | [Plugin: bgnow & direction](16-plugin-bgnow.md) | Current BG, trend arrows, stale data detection |
| 17 | [Plugin: IOB](17-plugin-iob.md) | Insulin on board calculation |
| 18 | [Plugin: COB](18-plugin-cob.md) | Carbs on board calculation |
| 19 | [Plugin: AR2 prediction](19-plugin-ar2.md) | Predictive glucose alerts |
| 20 | [Plugin: pump & loop status](20-plugin-pump-loop.md) | OpenAPS/Loop/AAPS status display |
| 21 | [Plugin: consumable ages](21-plugin-ages.md) | CAGE, SAGE, IAGE, BAGE |
| 22 | [Alarm engine](22-alarm-engine.md) | Urgent/warning/info alarm levels, snoozing |
| 23 | [LibreLink Up bridge](23-data-bridges.md) | Built-in LibreLink Up polling, primary data source |
| 24 | [Observability & monitoring](24-observability.md) | OpenTelemetry, Prometheus metrics, Boot Admin |
| 25 | [Deployment](25-deployment.md) | k3s manifests, sealed secrets, CI/CD pipeline |
| 26 | [Data migration tool](26-data-migration.md) | One-shot import from existing MongoDB Nightscout |
| 27 | [Integration testing](27-integration-testing.md) | Testcontainers suite, uploader compatibility tests |
| 28 | [Frontend: React SPA](28-frontend.md) | React + TypeScript SPA (after backend is stable) |
