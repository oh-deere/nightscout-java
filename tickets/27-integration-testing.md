# 27 — Integration Testing

**Priority:** P1 — must pass before go-live
**Depends on:** 06-11 (API v1 endpoints)
**Parallelizable with:** 22-26, 28

## Summary

End-to-end integration test suite that validates the service works with real CGM uploaders.

## Test Categories

### 1. API Compatibility Tests

Capture real HTTP requests from xDrip+, AAPS, and Loop, replay them against nightscout-java:

- **xDrip+ upload flow**: POST entries, GET status, verify auth
- **AAPS upload flow**: POST entries, POST treatments, POST devicestatus, GET profile
- **Loop upload flow**: POST devicestatus with Loop payload

Use Testcontainers PostgreSQL + `@SpringBootTest` with random port.

### 2. Data Integrity Tests

- POST entries → GET entries → verify exact match
- POST treatments → GET treatments → verify all fields preserved
- Deduplication: POST same entry twice → only one stored
- Date range queries return correct results
- Large batch insert (1000 entries) completes successfully

### 3. Auth Tests

- Requests with valid API_SECRET hash → 200
- Requests with invalid secret → 401
- Requests with valid JWT → 200
- Requests with expired JWT → 401
- Permission checks: read-only token can GET but not POST

### 4. Plugin Calculation Tests

- Known SGV sequence → verify IOB, COB, AR2 output matches upstream Nightscout
- These are the most critical tests — medical algorithm correctness

## Captured Payloads

Store real request/response pairs in `src/test/resources/payloads/`:
```
payloads/
├── xdrip/
│   ├── entries-post.json
│   └── status-get.json
├── aaps/
│   ├── entries-post.json
│   ├── treatments-post.json
│   └── devicestatus-post.json
└── loop/
    └── devicestatus-post.json
```

Capture these from a running Nightscout instance using request logging.

## Acceptance Criteria

- [ ] Full upload flow tests for xDrip+ and AAPS
- [ ] Data round-trip integrity verified
- [ ] Deduplication verified
- [ ] Auth flow verified
- [ ] Plugin calculations verified against upstream reference values
- [ ] All tests run in CI with Testcontainers (no external dependencies)
