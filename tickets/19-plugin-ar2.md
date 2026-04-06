# 19 — Plugin: AR2 Prediction

**Priority:** P2
**Depends on:** 06 (entries)
**Parallelizable with:** 16-18, 20, 21

## Summary

Port the AR2 (Auto-Regressive order 2) predictive alert plugin — predicts future glucose values and triggers alarms before actual highs/lows occur.

## Algorithm

AR2 uses a second-order auto-regressive model:
1. Take the last 2 SGV readings
2. Extrapolate forward using the current rate of change and acceleration
3. If predicted BG crosses alarm thresholds within the forecast window (default 30 min), trigger a predictive alarm

### Formula (simplified)
```
predicted[t+5] = a * sgv[now] + b * sgv[now-5] + noise
```

Where `a` and `b` are AR(2) coefficients derived from recent readings.

Uses Monte Carlo simulation with noise to estimate probability of crossing thresholds.

## Acceptance Criteria

- [ ] AR2 prediction generates forecast points
- [ ] Predictive alarms trigger when forecast crosses high/low thresholds
- [ ] Forecast data available via API for frontend display
- [ ] Unit tests with known SGV sequences

## Notes

- Reference: `lib/plugins/ar2.js` in cgm-remote-monitor.
- Lower priority than IOB/COB since AAPS/Loop do their own predictions. Useful for non-looping users.
