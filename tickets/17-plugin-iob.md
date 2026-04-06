# 17 — Plugin: Insulin on Board (IOB)

**Priority:** P1 — critical medical algorithm
**Depends on:** 07 (treatments), 08 (profiles)
**Parallelizable with:** 16, 18, 19, 20, 21

## Summary

Port the IOB (Insulin on Board) calculation — determines how much active insulin remains from previous boluses and temp basals.

## Algorithm

IOB uses an insulin activity curve to calculate remaining insulin:

1. Gather all insulin treatments (boluses + temp basals) within the DIA (Duration of Insulin Action) window (typically 3-6 hours)
2. For each treatment, calculate remaining active insulin using the selected activity curve
3. Sum all active insulin = current IOB

### Activity Curves

Nightscout supports multiple curves (configurable):
- **Bilinear** (default) — simple two-phase linear decay
- **Rapid-acting** — exponential decay, more physiologically accurate
- **Ultra-rapid** — for Fiasp/Lyumjev insulins

Key parameter: **DIA** (Duration of Insulin Action) from the active profile, typically 3-5 hours.

### Inputs
- Treatments with `insulin > 0` within DIA window
- Temp basal treatments (calculate insulin delivered above/below scheduled basal)
- Active profile (for DIA, scheduled basal rates)

### Output
```json
{
  "iob": 1.55,
  "activity": 0.023,
  "basaliob": 0.3,
  "bolusiob": 1.25
}
```

## Acceptance Criteria

- [ ] IOB calculation with bilinear curve
- [ ] IOB calculation with rapid-acting curve
- [ ] Temp basal IOB (insulin above/below scheduled basal)
- [ ] Respects DIA from active profile
- [ ] Unit tests comparing output against upstream Nightscout for known inputs
- [ ] Handles edge cases: no treatments, overlapping temp basals, profile switches

## Notes

- This is a **medical algorithm** — correctness is critical. Port carefully and validate against upstream Nightscout output.
- Consider extracting the curve math into a pure function with no dependencies for easy testing.
- Reference: `lib/plugins/iob.js` and `lib/plugins/insulinactivity.js` in cgm-remote-monitor.
