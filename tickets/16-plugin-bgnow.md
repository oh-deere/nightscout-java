# 16 — Plugin: BG Now & Direction

**Priority:** P1 — foundational for all other plugins
**Depends on:** 06
**Parallelizable with:** 17, 18, 19, 20, 21

## Summary

Port the `bgnow` and `direction` plugins — current blood glucose value, trend direction, and stale data detection.

## Logic

### BG Now
- Read the latest SGV entry
- Determine if it's "stale" (older than configurable threshold, default 15 min)
- Calculate delta from previous reading
- Classify BG level: urgent high, high, in range, low, urgent low (based on thresholds from settings)

### Direction
- Map the `direction` string to a trend arrow character
- Directions: `DoubleUp`, `SingleUp`, `FortyFiveUp`, `Flat`, `FortyFiveDown`, `SingleDown`, `DoubleDown`, `NOT COMPUTABLE`, `RATE OUT OF RANGE`

### Stale Data
- If latest SGV is older than `ALARM_TIMEAGO_WARN` (default 15 min) → warning
- If older than `ALARM_TIMEAGO_URGENT` (default 30 min) → urgent

## Acceptance Criteria

- [ ] Service that returns current BG, delta, direction, stale status
- [ ] BG level classification using configurable thresholds
- [ ] Direction mapping to arrow characters/strings
- [ ] Stale data detection with configurable timeouts
- [ ] Unit tests with various BG scenarios
