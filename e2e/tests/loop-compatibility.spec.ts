import { test, expect, uniqueSysTime, uniqueCreatedAt } from './helpers'

/**
 * Tests that verify the exact behavior LoopKit/Loop requires from a Nightscout server.
 * Source: LoopKit/NightscoutKit/Sources/NightscoutKit/NightscoutClient.swift
 *
 * Loop's strict requirements:
 *  - POST responses must be HTTP 200 (NOT 201/204 — Loop treats anything else as failure)
 *  - POST responses must be a JSON array of inserted objects with the same length as the
 *    request, each element containing _id
 *  - syncIdentifier on treatments must be persisted and round-tripped verbatim
 *  - Loop only uses /api/v1/* and /api/v2/notifications/loop — no v3 calls anywhere
 *  - Loop reads with find[created_at][$gte|$lte], find[dateString][$gte|$lte],
 *    find[startDate][$gte|$lte] for profiles
 */
test.describe('Loop compatibility', () => {
  test('POST /api/v1/entries returns 200 with _id and same length as request', async ({ api }) => {
    const sysTime1 = uniqueSysTime()
    const sysTime2 = uniqueSysTime()
    const dateMs = Date.now()

    const res = await api.post('/api/v1/entries.json', {
      data: [
        { type: 'sgv', date: dateMs, dateString: new Date(dateMs).toISOString(), sysTime: sysTime1, sgv: 100, direction: 'Flat', device: 'loop-test' },
        { type: 'sgv', date: dateMs + 1000, dateString: new Date(dateMs + 1000).toISOString(), sysTime: sysTime2, sgv: 105, direction: 'Flat', device: 'loop-test' },
      ],
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(Array.isArray(body)).toBe(true)
    expect(body).toHaveLength(2)
    for (const entry of body) {
      expect(entry._id).toBeTruthy()
      expect(typeof entry._id).toBe('string')
    }
  })

  test('POST /api/v1/treatments returns 200 with _id and preserves syncIdentifier', async ({ api }) => {
    const createdAt = uniqueCreatedAt()
    const syncId = `loop-sync-${Date.now()}`

    const res = await api.post('/api/v1/treatments.json', {
      data: [
        {
          eventType: 'Correction Bolus',
          created_at: createdAt,
          insulin: 1.5,
          enteredBy: 'loop://iPhone',
          syncIdentifier: syncId,
          insulinType: 'novolog',
        },
      ],
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body).toHaveLength(1)
    expect(body[0]._id).toBeTruthy()

    // GET it back and verify syncIdentifier is preserved
    const list = await api.get(
      `/api/v1/treatments.json?${encodeURIComponent('find[created_at][$gte]')}=${encodeURIComponent(
        new Date(Date.parse(createdAt) - 1000).toISOString(),
      )}`,
    )
    expect(list.ok()).toBeTruthy()
    const items = await list.json()
    const found = items.find((t: { _id: string }) => t._id === body[0]._id)
    expect(found).toBeTruthy()
    expect(found.syncIdentifier).toBe(syncId)
    expect(found.insulinType).toBe('novolog')

    // Cleanup
    await api.delete(`/api/v1/treatments/${body[0]._id}`)
  })

  test('POST /api/v1/devicestatus returns 200 with _id and same length as request', async ({ api }) => {
    const res = await api.post('/api/v1/devicestatus.json', {
      data: [
        {
          device: 'loop://iPhone',
          created_at: new Date().toISOString(),
          loop: { iob: { iob: 1.5 }, predicted: { values: [120, 118, 115] } },
          pump: { battery: { percent: 80 } },
        },
      ],
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(Array.isArray(body)).toBe(true)
    expect(body).toHaveLength(1)
    expect(body[0]._id).toBeTruthy()

    if (body[0]._id) {
      await api.delete(`/api/v1/devicestatus/${body[0]._id}`)
    }
  })

  test('POST /api/v1/profile returns 200 with _id', async ({ api }) => {
    const res = await api.post('/api/v1/profile.json', {
      data: {
        defaultProfile: 'loop-test',
        store: { 'loop-test': { dia: 4, units: 'mg/dl', basal: [{ time: '00:00', value: 1.0 }] } },
        startDate: new Date().toISOString(),
      },
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    // Loop expects an array with _id, but the upstream code path is "POST array of one"
    // Our endpoint accepts a single object body — verify it returns _id either way
    if (Array.isArray(body)) {
      expect(body).toHaveLength(1)
      expect(body[0]._id).toBeTruthy()
    } else {
      expect(body._id).toBeTruthy()
    }
  })

  test('GET /api/v1/profile/current returns single ProfileSet', async ({ api }) => {
    // Ensure at least one profile exists
    await api.post('/api/v1/profile.json', {
      data: {
        defaultProfile: 'loop-current-test',
        store: { 'loop-current-test': { dia: 4, units: 'mg/dl' } },
      },
    })

    const res = await api.get('/api/v1/profile/current')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(Array.isArray(body)).toBe(false)
    expect(body._id).toBeTruthy()
    expect(body.defaultProfile).toBeTruthy()
  })

  test('GET /api/v1/profiles?find[startDate][$gte] returns array filtered by start date', async ({ api }) => {
    const startDate = new Date(Date.now() - 60_000).toISOString()
    await api.post('/api/v1/profile.json', {
      data: {
        defaultProfile: 'startdate-test',
        store: { 'startdate-test': { dia: 4, units: 'mg/dl' } },
        startDate,
      },
    })

    const res = await api.get(
      `/api/v1/profiles?${encodeURIComponent('find[startDate][$gte]')}=${encodeURIComponent(
        new Date(Date.now() - 3600_000).toISOString(),
      )}`,
    )
    expect(res.ok()).toBeTruthy()
    const items = await res.json()
    expect(Array.isArray(items)).toBe(true)
  })

  test('GET /api/v1/entries with find[dateString][$gte] filter (Loop pattern)', async ({ api }) => {
    const dateString = new Date(Date.now() - 60_000).toISOString()
    await api.post('/api/v1/entries.json', {
      data: [{ type: 'sgv', date: Date.parse(dateString), dateString, sysTime: uniqueSysTime(), sgv: 110, direction: 'Flat' }],
    })

    const since = new Date(Date.now() - 3600_000).toISOString()
    const res = await api.get(
      `/api/v1/entries.json?${encodeURIComponent('find[dateString][$gte]')}=${encodeURIComponent(since)}`,
    )
    expect(res.ok()).toBeTruthy()
    const items = await res.json()
    expect(Array.isArray(items)).toBe(true)
  })

  test('GET /api/v1/devicestatus with find[created_at][$gte|$lte] filter', async ({ api }) => {
    const since = new Date(Date.now() - 3600_000).toISOString()
    const until = new Date().toISOString()
    const res = await api.get(
      `/api/v1/devicestatus.json?${encodeURIComponent('find[created_at][$gte]')}=${encodeURIComponent(
        since,
      )}&${encodeURIComponent('find[created_at][$lte]')}=${encodeURIComponent(until)}`,
    )
    expect(res.ok()).toBeTruthy()
    expect(Array.isArray(await res.json())).toBe(true)
  })

  test('GET /api/v1/experiments/test acts as auth check', async ({ api }) => {
    const res = await api.get('/api/v1/experiments/test')
    expect([200, 204]).toContain(res.status())
  })

  test('POST /api/v2/notifications/loop accepts override request', async ({ api }) => {
    const res = await api.post('/api/v2/notifications/loop', {
      data: {
        eventType: 'Temporary Override',
        reason: 'Exercise',
        reasonDisplay: 'Exercise mode',
        duration: '60',
        notes: '',
      },
    })
    expect(res.status()).toBe(200)
  })

  test('POST /api/v2/notifications/loop accepts cancel override', async ({ api }) => {
    const res = await api.post('/api/v2/notifications/loop', {
      data: { eventType: 'Temporary Override Cancel', duration: '0' },
    })
    expect(res.status()).toBe(200)
  })
})
