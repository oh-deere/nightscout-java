# 24 — Observability & Monitoring

**Priority:** P1
**Depends on:** 01
**Parallelizable with:** all other tickets

## Summary

Set up observability following OhDeere conventions: OpenTelemetry, Prometheus metrics, Spring Boot Admin.

## Components

### Spring Boot Actuator
- Health endpoint (`/actuator/health`) — DB connectivity, disk space
- Info endpoint (`/actuator/info`) — version, git commit
- Prometheus endpoint (`/actuator/prometheus`)

### Prometheus Metrics

Custom metrics to expose:
- `nightscout_entries_total` — total entries by type (sgv, mbg, cal)
- `nightscout_entries_latest_age_seconds` — age of most recent SGV (stale data detection)
- `nightscout_treatments_total` — total treatments by event type
- `nightscout_websocket_connections` — active WebSocket connections
- `nightscout_api_requests_total` — requests by endpoint and status
- `nightscout_bridge_polls_total` — Dexcom bridge poll count (if enabled)
- `nightscout_current_sgv` — current glucose value (for Grafana dashboards)

### OpenTelemetry
- Distributed tracing on all REST endpoints and DB queries
- Span attributes: collection name, query type, result count

### Spring Boot Admin
- Register with Boot Admin at the standard OhDeere endpoint
- Client dependency: `de.codecentric:spring-boot-admin-starter-client:4.0.2`

## Acceptance Criteria

- [ ] Actuator health/info/prometheus endpoints enabled
- [ ] Custom Prometheus metrics for CGM-specific data
- [ ] OpenTelemetry tracing configured
- [ ] Spring Boot Admin client registered
- [ ] Pod annotations for Prometheus scraping
- [ ] Grafana dashboard definition for glucose monitoring (optional)
