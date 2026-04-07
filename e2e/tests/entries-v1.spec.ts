import { test, expect, uniqueSysTime } from './helpers'

test.describe('API v1 — entries', () => {
  test('POST /api/v1/entries → GET round-trip with deduplication', async ({ api }) => {
    const sysTime = uniqueSysTime()
    const dateMs = Date.now()
    const dateString = new Date(dateMs).toISOString()

    // POST a single SGV
    const post = await api.post('/api/v1/entries.json', {
      data: [
        {
          type: 'sgv',
          date: dateMs,
          dateString,
          sysTime,
          sgv: 142,
          direction: 'Flat',
          device: 'e2e-test',
          noise: 1,
        },
      ],
    })
    expect(post.ok()).toBeTruthy()
    const saved = await post.json()
    expect(saved).toHaveLength(1)
    expect(saved[0].sgv).toBe(142)
    expect(saved[0]._id).toBeTruthy()
    const id = saved[0]._id

    // GET by id
    const getOne = await api.get(`/api/v1/entries/${id}`)
    expect(getOne.ok()).toBeTruthy()
    const fetched = await getOne.json()
    expect(fetched.sgv).toBe(142)
    expect(fetched._id).toBe(id)

    // POST the same sysTime again — should be deduplicated (response is empty array)
    const dup = await api.post('/api/v1/entries.json', {
      data: [
        { type: 'sgv', date: dateMs, dateString, sysTime, sgv: 142, direction: 'Flat', device: 'e2e-test', noise: 1 },
      ],
    })
    expect(dup.ok()).toBeTruthy()
    const dupBody = await dup.json()
    expect(dupBody).toHaveLength(0)

    // Cleanup
    await api.delete(`/api/v1/entries/${id}`)
  })

  test('GET /api/v1/entries/current.json returns the latest SGV', async ({ api }) => {
    const sysTime = uniqueSysTime()
    const dateMs = Date.now() + 60_000 // ensure newer than anything else
    await api.post('/api/v1/entries.json', {
      data: [{ type: 'sgv', date: dateMs, dateString: new Date(dateMs).toISOString(), sysTime, sgv: 199, direction: 'Flat', device: 'e2e-test' }],
    })
    const res = await api.get('/api/v1/entries/current.json')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body[0].sgv).toBe(199)
  })

  test('GET /api/v1/entries/sgv.json returns sgv-typed entries', async ({ api }) => {
    const res = await api.get('/api/v1/entries/sgv.json?count=5')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(Array.isArray(body)).toBeTruthy()
    for (const e of body) {
      expect(e.type).toBe('sgv')
    }
  })

  test('GET /api/v1/entries with find[date][$gte] filter', async ({ api }) => {
    const res = await api.get(
      `/api/v1/entries.json?${encodeURIComponent('find[date][$gte]')}=${Date.now() - 3600_000}`,
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(Array.isArray(body)).toBeTruthy()
  })

  test('DELETE /api/v1/entries/{id}', async ({ api }) => {
    const sysTime = uniqueSysTime()
    const post = await api.post('/api/v1/entries.json', {
      data: [{ type: 'sgv', date: Date.now(), dateString: new Date().toISOString(), sysTime, sgv: 100 }],
    })
    const id = (await post.json())[0]._id
    const del = await api.delete(`/api/v1/entries/${id}`)
    expect(del.ok()).toBeTruthy()
    const get = await api.get(`/api/v1/entries/${id}`)
    expect(get.status()).toBe(404)
  })

  test('GET /api/v1/entries/{invalid-uuid} returns 404', async ({ api }) => {
    const res = await api.get('/api/v1/entries/not-a-uuid')
    expect(res.status()).toBe(404)
  })
})
