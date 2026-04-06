# CLAUDE.md — nightscout-java

Nightscout CGM server rewritten in Java 25 / Spring Boot 4 / PostgreSQL.

Read the parent [OhDeere CLAUDE.md](../CLAUDE.md) first — it has the hard requirements.

## Quick Reference

- **Base package:** `se.ohdeere.nightscout`
- **Java:** 25 (Temurin)
- **Spring Boot:** 4.0.3
- **Database:** PostgreSQL (`nightscout` database)
- **Build:** `./mvnw clean verify`
- **Run locally:** `./mvnw spring-boot:run` (needs local Postgres on `localhost:5432/nightscout`)
- **Image:** `10.10.2.181:30500/nightscout-java`

## Project-Specific Notes

- This service implements the **Nightscout API v1 and v3** contracts for compatibility with xDrip+, AAPS, Loop, and other CGM tools.
- **Primary data source:** LibreLink Up bridge (polls Abbott's cloud API). Configured via `LINK_UP_*` env vars.
- **Auth model:** Nightscout-native API_SECRET (SHA-1 hash) + JWT. Not the OhDeere OAuth2 auth server.
- **Plugins** (IOB, COB, AR2) contain **medical algorithms** — port carefully, validate against upstream Nightscout output.
- Tickets for planned work are in the `tickets/` directory.

## Database

Local development:
```sql
CREATE DATABASE nightscout;
CREATE USER nightscout WITH PASSWORD 'local';
GRANT ALL PRIVILEGES ON DATABASE nightscout TO nightscout;
```

## Formatting

Run `./mvnw spring-javaformat:apply` to auto-format before committing.
