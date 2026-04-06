# 18 — Plugin: Carbs on Board (COB)

**Priority:** P1 — critical medical algorithm
**Depends on:** 07 (treatments), 08 (profiles)
**Parallelizable with:** 16, 17, 19, 20, 21

## Summary

Port the COB (Carbs on Board) calculation — estimates remaining unabsorbed carbohydrates.

## Algorithm

1. Gather all carb treatments within the absorption window (typically 4-8 hours)
2. For each carb entry, calculate absorbed carbs based on carb absorption rate
3. Remaining COB = entered carbs - absorbed carbs

### Absorption Model

Nightscout uses a **linear decay** model by default:
- Carbs absorb at a constant rate based on the `carbs_hr` parameter (typically 20-30g/hour from profile)
- Can be overridden by real-time BG deviations (if BG rises faster than expected, carbs are absorbing faster)

### Inputs
- Treatments with `carbs > 0`
- Active profile (for `carbs_hr`, ISF, IC ratio)
- Recent SGV entries (for deviation-based absorption adjustment)

### Output
```json
{
  "cob": 25.5,
  "displayCob": 26
}
```

## Acceptance Criteria

- [ ] COB calculation with linear decay model
- [ ] Configurable `carbs_hr` absorption rate from profile
- [ ] Unit tests comparing output against upstream Nightscout
- [ ] Handles edge cases: multiple carb entries, zero carbs, very old carb entries

## Notes

- Same as IOB: medical algorithm, port carefully.
- Reference: `lib/plugins/cob.js` in cgm-remote-monitor.
