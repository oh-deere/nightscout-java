# 14 — API v3: Auth & Permissions

**Priority:** P1
**Depends on:** 05, 13
**Parallelizable with:** 12, 15

## Summary

Implement API v3-specific auth: JWT token lifecycle, subject-based access, and role resolution.

## Endpoints

### `GET /api/v3/authorization/request/{token}`

Exchange a long-lived token for a short-lived JWT.

### `POST /api/v2/authorization/request`

Request a JWT for a subject. Body: `{"token": "<api_secret_or_token>"}`.
Returns: `{"token": "<jwt>", "sub": "...", "permissionGroups": [...]}`.

## JWT Claims

```json
{
  "sub": "xdrip-uploader",
  "iat": 1705312200,
  "exp": 1705398600,
  "accessToken": "...",
  "permissionGroups": ["api:entries:create", "api:treatments:create"]
}
```

## Acceptance Criteria

- [ ] JWT issuance via v2 authorization endpoint (used by newer clients)
- [ ] JWT contains subject and permissions
- [ ] Token expiry and refresh
- [ ] Permission checking integrated with API v3 endpoints
- [ ] Each collection operation checks appropriate permission
