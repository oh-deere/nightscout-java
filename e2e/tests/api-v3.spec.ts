import { test, expect, uniqueCreatedAt, uniqueSysTime } from './helpers'

test.describe('API v3 — entries', () => {
  test('lastModified returns timestamps for all collections', async ({ api }) => {
    const res = await api.get('/api/v3/lastModified')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.collections).toBeDefined()
    expect(body.collections.entries).toBeDefined()
    expect(body.collections.treatments).toBeDefined()
    expect(body.collections.profile).toBeDefined()
    expect(body.collections.devicestatus).toBeDefined()
    expect(body.srvDate).toBeGreaterThan(0)
  })

  test('GET /api/v3/entries returns identifier-shaped objects', async ({ api }) => {
    const res = await api.get('/api/v3/entries?limit=5')
    expect(res.ok()).toBeTruthy()
    const items = await res.json()
    if (items.length > 0) {
      const e = items[0]
      expect(e.identifier).toBeTruthy()
      expect(e.srvModified).toBeDefined()
      expect(e.isValid).toBe(true)
    }
  })

  test('GET /api/v3/entries/{identifier} round-trip', async ({ api }) => {
    // Create via v1, fetch via v3
    const sysTime = uniqueSysTime()
    const post = await api.post('/api/v1/entries.json', {
      data: [{ type: 'sgv', date: Date.now(), sysTime, sgv: 110, direction: 'Flat' }],
    })
    const id = (await post.json())[0]._id

    const v3get = await api.get(`/api/v3/entries/${id}`)
    expect(v3get.ok()).toBeTruthy()
    const body = await v3get.json()
    expect(body.identifier).toBe(id)
    expect(body.sgv).toBe(110)

    await api.delete(`/api/v1/entries/${id}`)
  })
})

test.describe('API v3 — treatments CRUD', () => {
  test('full POST → GET → PUT → PATCH → DELETE round-trip', async ({ api }) => {
    const createdAt = uniqueCreatedAt()

    // CREATE
    const post = await api.post('/api/v3/treatments', {
      data: { eventType: 'Correction Bolus', created_at: createdAt, insulin: 1.5, enteredBy: 'e2e' },
    })
    expect(post.status()).toBe(201)
    const created = await post.json()
    expect(created.identifier).toBeTruthy()
    expect(created.insulin).toBe(1.5)
    const id = created.identifier

    // READ ONE
    const getOne = await api.get(`/api/v3/treatments/${id}`)
    expect(getOne.ok()).toBeTruthy()
    const fetched = await getOne.json()
    expect(fetched.eventType).toBe('Correction Bolus')

    // PUT (replace)
    const put = await api.put(`/api/v3/treatments/${id}`, {
      data: { eventType: 'Correction Bolus', created_at: createdAt, insulin: 2.0, notes: 'replaced' },
    })
    expect(put.ok()).toBeTruthy()
    const replaced = await put.json()
    expect(replaced.insulin).toBe(2.0)
    expect(replaced.notes).toBe('replaced')

    // PATCH (partial)
    const patch = await api.patch(`/api/v3/treatments/${id}`, {
      data: { notes: 'patched' },
    })
    expect(patch.ok()).toBeTruthy()
    const patched = await patch.json()
    expect(patched.notes).toBe('patched')
    expect(patched.insulin).toBe(2.0) // preserved

    // HISTORY since 1 hour ago should include this treatment
    const since = Date.now() - 3600_000
    const history = await api.get(`/api/v3/treatments/history/${since}`)
    expect(history.ok()).toBeTruthy()
    const historyItems = await history.json()
    expect(historyItems.find((t: { identifier: string }) => t.identifier === id)).toBeTruthy()

    // DELETE
    const del = await api.delete(`/api/v3/treatments/${id}`)
    expect(del.status()).toBe(204)
    const after = await api.get(`/api/v3/treatments/${id}`)
    expect(after.status()).toBe(404)
  })

  test('GET /api/v3/treatments/{not-uuid} returns 404', async ({ api }) => {
    const res = await api.get('/api/v3/treatments/not-a-uuid')
    expect(res.status()).toBe(404)
  })
})

test.describe('API v3 — profile CRUD', () => {
  test('POST → GET → PUT → DELETE', async ({ api }) => {
    const post = await api.post('/api/v3/profile', {
      data: {
        defaultProfile: 'V3-test',
        store: { 'V3-test': { units: 'mg/dl', dia: 4 } },
      },
    })
    expect(post.status()).toBe(201)
    const created = await post.json()
    expect(created.identifier).toBeTruthy()
    const id = created.identifier

    const getOne = await api.get(`/api/v3/profile/${id}`)
    expect(getOne.ok()).toBeTruthy()
    expect((await getOne.json()).defaultProfile).toBe('V3-test')

    const put = await api.put(`/api/v3/profile/${id}`, {
      data: {
        defaultProfile: 'V3-updated',
        store: { 'V3-updated': { units: 'mmol/l', dia: 5 } },
      },
    })
    expect(put.ok()).toBeTruthy()
    expect((await put.json()).defaultProfile).toBe('V3-updated')

    const del = await api.delete(`/api/v3/profile/${id}`)
    expect(del.status()).toBe(204)
  })
})

test.describe('API v3 — devicestatus CRUD', () => {
  test('POST → GET → DELETE', async ({ api }) => {
    const post = await api.post('/api/v3/devicestatus', {
      data: { device: 'e2e-v3', uploader: { battery: 50 } },
    })
    expect(post.status()).toBe(201)
    const created = await post.json()
    expect(created.identifier).toBeTruthy()
    const id = created.identifier

    const getOne = await api.get(`/api/v3/devicestatus/${id}`)
    expect(getOne.ok()).toBeTruthy()
    expect((await getOne.json()).device).toBe('e2e-v3')

    const del = await api.delete(`/api/v3/devicestatus/${id}`)
    expect(del.status()).toBe(204)
  })
})
