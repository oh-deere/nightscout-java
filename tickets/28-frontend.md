# 28 — Frontend: React + TypeScript SPA

**Priority:** P2 — after backend is stable and tested with existing Nightscout UI
**Depends on:** 06-15 (all API endpoints), 15 (WebSocket)
**Parallelizable with:** independent of backend work once APIs are stable

## Summary

Build a modern React + TypeScript SPA to replace the legacy Nightscout jQuery/D3 frontend. This will be a new project following OhDeere React conventions (Vite, TanStack Query, CSS Modules).

## Phased Approach

### Phase 1: Use existing Nightscout UI for testing

During backend development, serve the upstream Nightscout static frontend to validate API compatibility. This requires Socket.IO compatibility in the WebSocket layer (ticket 15). This is temporary — just for testing.

### Phase 2: Build the React SPA

New frontend as a separate project (or section in ohdeere-app), with:

#### Core Views

1. **Dashboard** — the main glucose monitoring screen
   - Current BG value, large and prominent
   - Trend arrow (direction)
   - Delta from previous reading
   - Time since last reading (stale data warning)
   - BG graph (last 3/6/12/24 hours, selectable)
   - IOB/COB pills (when available)
   - Pump/loop status (when available)
   - Consumable ages (CAGE, SAGE)

2. **Treatment Log** — recent treatments
   - Filterable by event type
   - Insulin and carb entries highlighted

3. **Reports** — basic analytics
   - Time in range (TIR) percentage
   - Average BG, estimated A1C
   - Daily/weekly overlay graphs
   - AGP (Ambulatory Glucose Profile) view

4. **Settings** — configure thresholds, plugins, display preferences

#### Tech Stack

- React 18+ with TypeScript
- Vite build tooling
- TanStack Query for data fetching
- WebSocket (STOMP) for real-time updates
- D3.js or Recharts for glucose graphs
- CSS Modules + Sass

#### Real-time Updates

- Connect to WebSocket on load
- New SGV readings appear on graph without page refresh
- Alarm notifications shown as browser notifications (with permission)

## Acceptance Criteria

- [ ] Dashboard view with current BG, trend, delta, graph
- [ ] BG graph with selectable time ranges
- [ ] Real-time updates via WebSocket
- [ ] Treatment log view
- [ ] Time in range report
- [ ] Responsive design (phone + desktop)
- [ ] Dark mode support
- [ ] Browser notification for alarms

## Notes

- The existing Nightscout UI is ~1.5MB of jQuery/D3 — rewriting in React with modern tooling will be significantly smaller and more maintainable.
- Focus on the dashboard view first. That's 90% of the usage.
- Consider deploying as a static SPA on Netlify/Cloudflare (like ohdeere-app) rather than bundling with the Java service.
