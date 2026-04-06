# 23 — LibreLink Up Bridge

**Priority:** P0 — primary data source
**Depends on:** 04 (storage layer)
**Parallelizable with:** 06-11 (API endpoints)

## Summary

Built-in bridge that polls LibreLink Up (Abbott's cloud API) for FreeStyle Libre CGM readings and writes them directly to PostgreSQL. Replaces the separate [nightscout-librelink-up](https://github.com/timoschlueter/nightscout-librelink-up) Node.js service.

This is the **primary data source** for this instance — not a secondary bridge.

## LibreLink Up API

### Auth Flow

1. `POST https://{region-host}/llu/auth/login` with `{"email": "...", "password": "..."}`
2. If response has `redirect=true` + `region`, switch to correct regional host and retry
3. Response contains `authTicket` (`token`, `expires`, `duration`) and `user.id`
4. Subsequent requests use `Authorization: Bearer {token}` and `account-id: SHA256(userId)`

**Required headers on all requests:**
```
User-Agent: Mozilla/5.0 (iPhone; CPU OS 17_4.1 like Mac OS X) ...
Content-Type: application/json;charset=UTF-8
version: 4.16.0
product: llu.ios
```

### Data Fetch (two calls per cycle)

**1. Get connections:** `GET /llu/connections`
Returns patient connections. Cache the `patientId` after first call.

**2. Get graph data:** `GET /llu/connections/{patientId}/graph`
Returns:
- `connection.glucoseMeasurement` — current reading with `TrendArrow`
- `graphData[]` — historical `GlucoseItem` objects

### GlucoseItem Fields

```json
{
  "FactoryTimestamp": "1/15/2024 10:30:00 AM",
  "Timestamp": "1/15/2024 11:30:00 AM",
  "ValueInMgPerDl": 120,
  "TrendArrow": 3,
  "MeasurementColor": 1,
  "isHigh": false,
  "isLow": false
}
```

**TrendArrow mapping:**
| Value | Nightscout Direction |
|-------|---------------------|
| 1 | SingleDown |
| 2 | FortyFiveDown |
| 3 | Flat |
| 4 | FortyFiveUp |
| 5 | SingleUp |

Note: `TrendArrow` is only present on the current measurement, not historical graph data.

### Timestamp Handling

`FactoryTimestamp` is in the sensor's factory timezone (not UTC). Must subtract the local timezone offset to get true UTC. Use explicit `ZoneId` parsing — do not rely on server timezone.

### Regional API Hosts

| Region | Host |
|--------|------|
| EU | `api-eu.libreview.io` |
| EU2 | `api-eu2.libreview.io` |
| DE | `api-de.libreview.io` |
| FR | `api-fr.libreview.io` |
| US | `api-us.libreview.io` |
| CA | `api-ca.libreview.io` |
| AU | `api-au.libreview.io` |
| AE | `api-ae.libreview.io` |
| AP | `api-ap.libreview.io` |
| JP | `api-jp.libreview.io` |
| LA | `api-la.libreview.io` |

## Implementation

### Spring Components

```java
// remote/librelink/LibreLinkUpClient.java
public interface LibreLinkUpClient {
    AuthTicket login(String email, String password);
    List<Connection> getConnections(AuthTicket ticket);
    GraphData getGraph(AuthTicket ticket, String patientId);
}

// service/bridge/LibreLinkUpBridgeService.java
@Service
class LibreLinkUpBridgeService {
    @Scheduled(fixedRateString = "${librelink.poll-interval-ms:300000}")
    void poll() {
        // 1. Ensure authenticated (login or use cached token)
        // 2. Fetch graph data
        // 3. Filter readings newer than last stored entry
        // 4. Transform to Entry records
        // 5. Batch insert via EntryRepository
        // 6. Broadcast new entries via WebSocket (if enabled)
    }
}
```

### Deduplication

Before inserting, query the latest entry's `date_ms` and only insert readings newer than that. The `UNIQUE (sys_time, type)` constraint is the safety net.

### Error Handling

- **401 Unauthorized** → clear cached token, re-login on next cycle
- **429 Rate Limited** → back off exponentially, log warning. This is the biggest operational risk with LibreLink Up.
- **Network errors** → retry with configurable attempts and interval
- **ToS acceptance required** → log error with instruction to accept in official app

## Configuration

```yaml
librelink:
  enabled: true
  username: ${LINK_UP_USERNAME}
  password: ${LINK_UP_PASSWORD}
  region: ${LINK_UP_REGION:EU}
  poll-interval-ms: ${LINK_UP_POLL_INTERVAL_MS:300000}  # 5 minutes
  connection-id: ${LINK_UP_CONNECTION:}  # optional, for multi-patient accounts
  retry-attempts: ${LINK_UP_RETRY_ATTEMPTS:3}
  retry-interval-ms: ${LINK_UP_RETRY_INTERVAL_MS:30000}
```

## Acceptance Criteria

- [ ] `LibreLinkUpClient` authenticates and fetches data from LibreLink Up API
- [ ] Auth token cached in memory, re-login on expiry or 401
- [ ] Regional redirect handled (login returns `redirect=true`)
- [ ] Graph data transformed to `Entry` records and stored in PostgreSQL
- [ ] `TrendArrow` mapped to Nightscout direction strings
- [ ] Timestamps correctly converted to UTC regardless of server timezone
- [ ] Deduplication: only new readings inserted
- [ ] Scheduled polling at configurable interval (default 5 min)
- [ ] 429 rate limiting handled with exponential backoff
- [ ] Bridge disabled gracefully when `librelink.enabled=false`
- [ ] Integration test with mocked LibreLink Up responses
- [ ] Metrics: `nightscout_bridge_polls_total`, `nightscout_bridge_readings_total`, `nightscout_bridge_errors_total`

## Known Risks

1. **Rate limiting / account locking** — LibreLink Up aggressively rate-limits. 5-minute polling is safe; shorter intervals risk account lock.
2. **TLS fingerprinting** — LibreLink Up uses Cloudflare. Java's `HttpClient` has a different TLS fingerprint than the iOS app. May need to test and potentially customize cipher suite order.
3. **ToS changes** — Abbott periodically updates ToS. API returns errors until user accepts in official app. Cannot be handled programmatically.
4. **Unofficial API** — this is reverse-engineered, not a supported API. Can break at any time.
