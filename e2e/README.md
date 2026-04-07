# nightscout-java end-to-end tests

Playwright API tests that exercise the running Spring Boot service over HTTP. Each test
issues real requests against a real database — no mocks, no in-memory shortcuts.

## Running

The tests assume the backend is up at `http://localhost:8090` with `API_SECRET=Jq6RH1NjytCPjnV`
(matches `application-local.properties`). Start it the usual way in another terminal:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Then from this directory:

```bash
npm install        # first time only
npm test           # run the suite
npm run report     # open the HTML report
```

To target a different host or secret:

```bash
BASE_URL=http://noah.ohdeere.se API_SECRET=hunter2 npm test
```

## What's covered

- **Status & auth** — `/api/versions`, `/api/v1/status.json`, `verifyauth`, `/api/v1/entries`
  with and without auth, JWT exchange via both v2 and v3 paths
- **Entries v1** — POST/GET/DELETE round-trip, deduplication, current, sgv, find filters,
  one-by-id lookup
- **Treatments v1** — POST/GET/PUT/DELETE round-trip, find filters, 400/404 edge cases
- **Profile v1** — POST/GET/PUT, `/profiles` alias, `/profile/current`, edge cases
- **Devicestatus v1** — POST/GET/DELETE
- **Activity v1** — POST/GET/PUT/DELETE
- **Properties & notifications** — plugin properties, notification ack via POST and GET
- **API v3** — `lastModified` for all collections, entries by identifier, full
  treatments/profile/devicestatus CRUD, history endpoints

## How tests stay isolated

Tests run sequentially against a shared database (no migrations between runs). Each test
uses a unique `sysTime` / `created_at` so it doesn't collide with other tests' records on
the unique constraints. Cleanup is best-effort via DELETE — orphaned test data is harmless
because every record uses obvious markers like `e2e-test`.
