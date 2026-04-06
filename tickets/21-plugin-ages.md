# 21 — Plugin: Consumable Ages (CAGE, SAGE, IAGE, BAGE)

**Priority:** P2
**Depends on:** 07 (treatments)
**Parallelizable with:** 16-20

## Summary

Port the consumable age plugins that track how old various diabetes supplies are.

## Plugins

| Plugin | Tracks | Treatment Event Type | Warning | Urgent |
|--------|--------|---------------------|---------|--------|
| CAGE | Cannula/infusion site age | `Site Change` | 48h | 72h |
| SAGE | Sensor age | `Sensor Start` / `Sensor Change` | 164h (6.8d) | 166h |
| IAGE | Insulin age (reservoir) | `Insulin Change` | 72h | 96h |
| BAGE | Battery age (pump) | `Pump Battery Change` | 336h (14d) | 504h (21d) |

## Logic

For each plugin:
1. Find the most recent treatment with the matching event type
2. Calculate hours elapsed since that treatment
3. Compare against warning/urgent thresholds
4. Return age, level (ok/warn/urgent), and display string

## Acceptance Criteria

- [ ] All four age plugins implemented
- [ ] Configurable warning/urgent thresholds via env vars
- [ ] Returns age in hours, display string ("2d 5h"), and alert level
- [ ] Unit tests
