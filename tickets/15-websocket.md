# 15 — WebSocket: Real-time Data Push

**Priority:** P1 — used by the web UI and some clients
**Depends on:** 06 (entries must be stored first)
**Parallelizable with:** 12, 13, 14

## Summary

Implement real-time data push when new entries/treatments arrive. Nightscout uses Socket.IO for this.

## Behavior

When a new SGV entry is posted:
1. Store it in the database
2. Broadcast to all connected WebSocket clients
3. Clients receive the data without polling

### Events

| Event | Payload | Trigger |
|-------|---------|---------|
| `dataUpdate` | `{sgvs: [...], treatments: [...], ...}` | New data stored |
| `alarm` | `{level: 2, title: "Urgent High", ...}` | Alarm triggered |
| `announcement` | `{title: "...", message: "..."}` | Announcement treatment |
| `clear_alarm` | `{level: ...}` | Alarm acknowledged |

## Implementation Options

1. **Socket.IO Java** — Use `netty-socketio` or `socket.io-server-java` to be wire-compatible with existing Nightscout web UI
2. **Spring WebSocket + STOMP** — Standard Spring approach, but requires changes to any client expecting Socket.IO protocol
3. **Hybrid** — Socket.IO for backwards compatibility, STOMP for new clients

**Recommendation:** Start with Spring WebSocket/STOMP for new clients. Add Socket.IO compatibility layer if you plan to serve the existing Nightscout web frontend.

## Acceptance Criteria

- [ ] WebSocket endpoint that clients can subscribe to
- [ ] New entries/treatments broadcast to connected clients in real-time
- [ ] Alarm events broadcast when thresholds crossed
- [ ] Auth on WebSocket connections (token-based)
- [ ] Connection handling: reconnect, heartbeat, cleanup

## Notes

- If we build a new React frontend (ticket 28), STOMP is cleaner and has better Spring integration.
- If we want to serve the existing Nightscout JS frontend, we need Socket.IO wire compatibility.
- Start with STOMP, add Socket.IO later if needed.
