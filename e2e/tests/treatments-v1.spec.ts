import { test, expect, uniqueCreatedAt } from './helpers'

test.describe('API v1 — treatments', () => {
  test('POST → GET → PUT → DELETE round-trip', async ({ api }) => {
    const createdAt = uniqueCreatedAt()

    // CREATE
    const post = await api.post('/api/v1/treatments.json', {
      data: [
        {
          eventType: 'Meal Bolus',
          created_at: createdAt,
          carbs: 30,
          insulin: 2.5,
          enteredBy: 'e2e-test',
        },
      ],
    })
    expect(post.ok()).toBeTruthy()
    const saved = await post.json()
    expect(saved).toHaveLength(1)
    expect(saved[0].carbs).toBe(30)
    expect(saved[0].insulin).toBe(2.5)
    const id = saved[0]._id

    // READ — list since just before now
    const list = await api.get(
      `/api/v1/treatments.json?${encodeURIComponent('find[created_at][$gte]')}=${encodeURIComponent(
        new Date(Date.parse(createdAt) - 1000).toISOString(),
      )}`,
    )
    expect(list.ok()).toBeTruthy()
    const items = await list.json()
    expect(items.find((t: { _id: string }) => t._id === id)).toBeTruthy()

    // UPDATE — change the carbs and add a note
    const put = await api.put('/api/v1/treatments.json', {
      data: { _id: id, eventType: 'Meal Bolus', created_at: createdAt, carbs: 45, notes: 'updated by e2e' },
    })
    expect(put.ok()).toBeTruthy()
    const updated = await put.json()
    expect(updated.carbs).toBe(45)
    expect(updated.notes).toBe('updated by e2e')
    expect(updated.insulin).toBe(2.5) // preserved

    // DELETE
    const del = await api.delete(`/api/v1/treatments/${id}`)
    expect(del.ok()).toBeTruthy()
  })

  test('PUT without _id returns 400', async ({ api }) => {
    const res = await api.put('/api/v1/treatments.json', {
      data: { eventType: 'Note', notes: 'no id' },
    })
    expect(res.status()).toBe(400)
  })

  test('PUT with non-existent id returns 404', async ({ api }) => {
    const res = await api.put('/api/v1/treatments.json', {
      data: { _id: '00000000-0000-0000-0000-000000000000', eventType: 'Note' },
    })
    expect(res.status()).toBe(404)
  })

  test('GET /api/v1/treatments with find[eventType] filter', async ({ api }) => {
    const createdAt = uniqueCreatedAt()
    await api.post('/api/v1/treatments.json', {
      data: [{ eventType: 'Site Change', created_at: createdAt }],
    })
    const res = await api.get(`/api/v1/treatments.json?${encodeURIComponent('find[eventType]')}=Site Change`)
    expect(res.ok()).toBeTruthy()
    const items = await res.json()
    expect(items.some((t: { eventType: string }) => t.eventType === 'Site Change')).toBeTruthy()
  })
})
