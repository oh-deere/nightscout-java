# 2. Do not implement server-push (Socket.IO / STOMP) for the React frontend

Date: 2026-04-07

## Status

Accepted

## Context

Upstream Nightscout broadcasts new entries, treatments, and alarms to connected
clients over a Socket.IO connection (`/socket.io` default namespace and
`/storage` + `/alarm` namespaces under api3). The legacy jQuery/D3 web UI relies
on this for real-time updates. Most third-party uploaders (xDrip+, AAPS, Loop) do
**not** use it — they upload via plain HTTP and rely on the server's REST API for
reads.

Our migration ticket plan included #15 *WebSocket — real-time data push* and
#22 *Alarm engine* with broadcast hooks. The current state:

- A `/ws` STOMP endpoint exists in `WebSocketConfig`, configured but never wired
  to any `@MessageMapping` handler.
- `DataUpdateBroadcaster` exists but is not consumed by any client we ship.
- Our React SPA (`frontend/`) consumes data exclusively via TanStack Query
  polling — there is no Socket.IO or STOMP client anywhere in the bundle.
- The PARITY report explicitly marks the legacy `/socket.io` namespace as
  "won't-do" because we built our own React UI.

Active alarms are surfaced through `/api/v1/properties` polling (every 60 s by
default). The dashboard's `AlarmBanner` displays them and offers per-type
snooze / unsnooze. With persistent snoozes (V4 migration) and a thirty-second
alarm-engine evaluation tick, the worst-case latency between an alarm condition
appearing and the user seeing it is roughly **30 s (engine eval) + 60 s
(properties poll) = ≤ 90 s**. For a CGM-class application this matches what
clients like xDrip+ and AAPS already provide via their own polling, and below
the granularity of the underlying CGM data (5-minute SGV cadence).

## Decision

We will **not** implement server-push (Socket.IO or STOMP broadcast handlers) for
client consumption. The frontend continues to use TanStack Query polling.

The existing `WebSocketConfig` and `DataUpdateBroadcaster` will be kept in place
as scaffolding for now (removing them is a separate cleanup) but will not be
fleshed out. Ticket #15 is closed as "intentionally not implemented".

If a future requirement demands sub-poll-interval latency (e.g. an embedded
display, a watch app, or a third-party real-time dashboard), the preferred
implementation is **Server-Sent Events (SSE)** rather than reviving Socket.IO:

- SSE is one-way (server → client), which matches the use case exactly.
- It speaks HTTP, so it works through every proxy, gateway, and CORS rule we
  already have configured.
- It does not require a client library — the browser `EventSource` API is
  enough.
- Spring MVC supports SSE natively via `SseEmitter` / `Flux<ServerSentEvent<T>>`,
  no additional config.

Socket.IO compatibility for the legacy upstream web UI is explicitly out of
scope (see PARITY.md "won't-do").

## Consequences

- **Pro:** zero ongoing maintenance for a transport we don't actually use; no
  Socket.IO compatibility shims, no STOMP message routing, no additional state
  on the server side.
- **Pro:** the frontend stays simple — no connection lifecycle, no reconnect
  logic, no out-of-order message handling.
- **Pro:** polling naturally rate-limits per client and degrades gracefully
  under load.
- **Con:** alarm latency is bounded by the polling interval. Users should be
  aware that the worst-case is ~90 s. We accept this trade-off because the
  underlying CGM data only updates every 5 minutes anyway.
- **Con:** we will never be a drop-in replacement for the legacy Nightscout web
  UI. That UI is not a target — we ship our own React frontend.
- **Future:** if push *is* eventually needed, add an SSE endpoint (e.g.
  `/api/v1/stream/properties`) instead of revisiting WebSockets. Update this
  ADR with a new one that supersedes it.
