# 22 — Alarm Engine

**Priority:** P1
**Depends on:** 16 (bgnow), 19 (ar2)
**Parallelizable with:** 23-28

## Summary

Implement the alarm system that triggers notifications when glucose crosses thresholds.

## Alarm Levels

| Level | Name | Example |
|-------|------|---------|
| 0 | None | BG in range |
| 1 | Info | BG approaching target boundary |
| 2 | Warning | BG above `BG_TARGET_TOP` or below `BG_TARGET_BOTTOM` |
| 3 | Urgent | BG above `BG_HIGH` or below `BG_LOW` |

## Alarm Types

1. **Simple** — trigger based on current BG vs thresholds
2. **Predict** — trigger based on AR2 predicted BG crossing thresholds within forecast window

Both types configurable via `ALARM_TYPES` env var (default: `["simple", "predict"]`).

## Features

- Alarm snoozing (per-alarm, configurable duration)
- Stale data alarm (no new SGV within `ALARM_TIMEAGO_WARN` minutes)
- Alarm broadcasting via WebSocket (ticket 15)
- Alarm acknowledgement via API (ticket 11)
- Plugin-based: any plugin can raise/clear alarms via the sandbox

## Acceptance Criteria

- [ ] Simple alarms trigger at configured BG thresholds
- [ ] Predictive alarms trigger based on AR2 forecast
- [ ] Stale data alarm when no fresh SGV
- [ ] Alarm snoozing with configurable duration
- [ ] Alarm state exposed via API and WebSocket
- [ ] Configurable alarm sounds/levels per threshold
- [ ] Unit tests for all alarm scenarios
