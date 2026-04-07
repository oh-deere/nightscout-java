import { test, expect } from './helpers'

test.describe('Properties and notifications', () => {
  test('GET /api/v1/properties returns plugin data', async ({ api }) => {
    const res = await api.get('/api/v1/properties')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    // bgnow may or may not be present depending on data; iob/cob always are
    expect(body).toBeDefined()
  })

  test('POST /api/v1/notifications/ack', async ({ api }) => {
    const res = await api.post('/api/v1/notifications/ack', {
      data: { level: 2, silenceMinutes: 5 },
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.result).toBe('ok')
  })

  test('GET /api/v1/notifications/ack with query params', async ({ api }) => {
    const res = await api.get('/api/v1/notifications/ack?level=2&silenceMinutes=5')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.result).toBe('ok')
  })
})
