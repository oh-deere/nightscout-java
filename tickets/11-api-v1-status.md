# 11 — API v1: Status & Misc Endpoints

**Priority:** P0 — status endpoint is checked by uploaders on connect
**Depends on:** 05
**Parallelizable with:** 06-10

## Summary

Implement status and utility endpoints that uploaders and clients check.

## Endpoints

### `GET /api/v1/status[.json]`

Returns server status. xDrip+ and AAPS check this on initial connection.

```json
{
  "status": "ok",
  "name": "nightscout",
  "version": "15.0.6",
  "serverTime": "2024-01-15T10:30:00.000Z",
  "serverTimeEpoch": 1705312200000,
  "apiEnabled": true,
  "careportalEnabled": true,
  "boluscalcEnabled": true,
  "settings": {
    "units": "mg/dl",
    "timeFormat": 24,
    "nightMode": false,
    "theme": "default",
    "language": "en",
    "showPlugins": "iob cob openaps pump cage sage iage",
    "showForecast": "ar2",
    "enable": ["iob", "cob", "openaps", "pump", "cage", "sage", "iage", "ar2"],
    "thresholds": { "bgHigh": 260, "bgTargetTop": 180, "bgTargetBottom": 80, "bgLow": 55 },
    "alarmTypes": ["simple", "predict"]
  }
}
```

### `GET /api/v1/verifyauth`

Verify authentication credentials. Returns `{"message":"OK"}` or 401.

### `POST /api/v1/notifications/ack`

Acknowledge an alarm notification.

### `GET /api/v1/experiments/test`

Simple test endpoint, returns `{"status":"ok"}`.

## Acceptance Criteria

- [ ] Status endpoint returns correct format (xDrip+ and AAPS parse specific fields)
- [ ] `version` field returns Nightscout-compatible version string
- [ ] `settings` populated from application configuration
- [ ] `/verifyauth` works with API secret and JWT
- [ ] Configuration via environment variables matching Nightscout conventions: `DISPLAY_UNITS`, `TIME_FORMAT`, `THEME`, `ENABLE`, `SHOW_PLUGINS`, `BG_HIGH`, `BG_TARGET_TOP`, `BG_TARGET_BOTTOM`, `BG_LOW`
- [ ] Spring RestDocs tests

## Notes

- xDrip+ specifically checks `status.apiEnabled` and `status.settings.units` on connect. If these are wrong, the uploader won't push data.
- The `ENABLE` env var is a space-separated list of plugin names — same as upstream.
