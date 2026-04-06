# 20 — Plugin: Pump & Loop Status

**Priority:** P1
**Depends on:** 09 (devicestatus)
**Parallelizable with:** 16-19, 21

## Summary

Port the pump and loop status plugins — display current pump state (battery, reservoir, status) and loop system state (OpenAPS/Loop/AAPS enacted changes, IOB, eventual BG).

## Data Source

Reads from latest `devicestatus` entries. Each loop system reports differently:

### OpenAPS / AAPS
```json
{
  "openaps": {
    "suggested": {"bg": 120, "eventualBG": 115, "IOB": 1.5, "reason": "..."},
    "enacted": {"rate": 0.8, "duration": 30, "recieved": true},
    "iob": {"iob": 1.5, "basaliob": 0.3}
  }
}
```

### Loop (iOS)
```json
{
  "loop": {
    "predicted": {"values": [120, 118, 115, ...]},
    "enacted": {"rate": 0.8, "duration": 30},
    "iob": {"iob": 1.5}
  }
}
```

### Pump status
```json
{
  "pump": {
    "battery": {"percent": 75},
    "reservoir": 120.5,
    "status": {"status": "normal", "bolusing": false}
  }
}
```

## Acceptance Criteria

- [ ] Extract pump status from latest devicestatus
- [ ] Extract loop/openaps status from latest devicestatus
- [ ] Detect stale loop data (loop not running / phone disconnected)
- [ ] Battery and reservoir warnings at configurable thresholds
- [ ] Supports OpenAPS, Loop, and AAPS payload formats
- [ ] Unit tests with real devicestatus payloads
