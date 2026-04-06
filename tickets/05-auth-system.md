# 05 — Auth System

**Priority:** P0 — blocks all secured endpoints
**Depends on:** 01
**Parallelizable with:** 04 (storage)

## Summary

Implement Nightscout's authentication system: API_SECRET hash-based auth and JWT tokens with shiro-style permissions.

## Nightscout Auth Model

Nightscout has a multi-layered auth system:

1. **API_SECRET** — a shared secret (env var). Clients send it as:
   - `api-secret` header (SHA-1 hash of the secret)
   - `?secret=<hash>` query parameter
   - `?token=<jwt>` query parameter
2. **JWT tokens** — issued by Nightscout, contain subject + permissions
3. **Shiro-style permissions** — e.g., `api:entries:read`, `api:treatments:create`, `api:*:*`

### Permission Format

```
api:<collection>:<action>
```

Actions: `read`, `create`, `update`, `delete`
Collections: `entries`, `treatments`, `profile`, `devicestatus`, `food`, `activity`, `settings`
Wildcard: `*` matches all

### Roles

| Role | Permissions |
|------|-------------|
| admin | `*:*:*` |
| readable | `api:*:read` |
| denied | (no permissions) |
| Custom | Any combination via `shiro-trie` |

## Implementation Plan

1. **API_SECRET verification** — compare SHA-1 hash of configured secret against `api-secret` header or `secret` query param
2. **JWT issuing** — `POST /api/v2/authorization/request` issues a JWT for a subject with defined permissions
3. **JWT verification** — validate JWT from `token` query param or `Authorization: Bearer` header
4. **Permission filter** — Spring Security filter chain that extracts auth context and checks permissions per endpoint
5. **Backwards compatibility** — API_SECRET grants full admin access (same as upstream Nightscout)

## Acceptance Criteria

- [ ] `API_SECRET` env var configured, SHA-1 hash compared on requests
- [ ] JWT issued and verified with configurable secret
- [ ] Shiro-style permission parsing and matching (with wildcards)
- [ ] Spring Security filter chain integrates both auth methods
- [ ] Unauthenticated requests get read-only access to status endpoint only
- [ ] Tests for: valid API secret, invalid secret, valid JWT, expired JWT, permission checks

## Notes

- The existing OhDeere auth server (`auth.ohdeere.se`) can optionally be used alongside Nightscout's native auth, but the native auth must work standalone for uploader compatibility.
- xDrip+ and AAPS send the API secret as a SHA-1 hash in the `api-secret` header — this must work exactly as upstream.
