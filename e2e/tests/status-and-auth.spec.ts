import { test, expect, apiSecretHash } from './helpers'

test.describe('Status and auth', () => {
  test('GET /api/versions', async ({ api }) => {
    const res = await api.get('/api/versions')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.versions).toContain('1')
    expect(body.versions).toContain('3')
  })

  test('GET /api/v1/status.json is public and returns settings', async ({ playwright, baseURL }) => {
    const ctx = await playwright.request.newContext({ baseURL })
    const res = await ctx.get('/api/v1/status.json')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.status).toBe('ok')
    expect(body.runtimeState).toBe('loaded')
    expect(body.settings).toBeDefined()
    expect(body.settings.thresholds).toBeDefined()
    await ctx.dispose()
  })

  test('GET /api/v1/entries.json without auth returns 401', async ({ playwright, baseURL }) => {
    const ctx = await playwright.request.newContext({ baseURL })
    const res = await ctx.get('/api/v1/entries.json')
    expect(res.status()).toBe(401)
    await ctx.dispose()
  })

  test('GET /api/v1/entries.json with valid api-secret returns 200', async ({ api }) => {
    const res = await api.get('/api/v1/entries.json?count=1')
    expect(res.ok()).toBeTruthy()
    expect(Array.isArray(await res.json())).toBeTruthy()
  })

  test('GET /api/v1/verifyauth with valid secret returns OK', async ({ api }) => {
    const res = await api.get('/api/v1/verifyauth')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.message).toBe('OK')
  })

  test('GET /api/v3/version is public', async ({ playwright, baseURL }) => {
    const ctx = await playwright.request.newContext({ baseURL })
    const res = await ctx.get('/api/v3/version')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.version).toBeTruthy()
    await ctx.dispose()
  })

  test('GET /api/v2/authorization/request/{token} exchanges secret for JWT', async ({ playwright, baseURL }) => {
    const ctx = await playwright.request.newContext({ baseURL })
    const res = await ctx.get(`/api/v2/authorization/request/${apiSecretHash}`)
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.token).toBeTruthy()
    expect(body.sub).toBeTruthy()
    expect(body.iat).toBeGreaterThan(0)
    expect(body.exp).toBeGreaterThan(body.iat)
    await ctx.dispose()
  })

  test('GET /api/v3/authorization/request/{token} alias also works', async ({ playwright, baseURL }) => {
    const ctx = await playwright.request.newContext({ baseURL })
    const res = await ctx.get(`/api/v3/authorization/request/${apiSecretHash}`)
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.token).toBeTruthy()
    await ctx.dispose()
  })

  test('GET /api/v2/authorization/request with invalid token returns 401', async ({ playwright, baseURL }) => {
    const ctx = await playwright.request.newContext({ baseURL })
    const res = await ctx.get('/api/v2/authorization/request/not-a-real-token')
    expect(res.status()).toBe(401)
    await ctx.dispose()
  })
})
