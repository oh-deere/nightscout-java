import { test, expect } from './helpers'

test.describe('API v1 — activity', () => {
  test('POST → GET → PUT → DELETE round-trip', async ({ api }) => {
    const post = await api.post('/api/v1/activity', {
      data: { name: 'e2e-walk', duration: 30 },
    })
    expect(post.ok()).toBeTruthy()
    const created = await post.json()
    expect(created._id).toBeTruthy()
    expect(created.duration).toBe(30)
    const id = created._id

    const list = await api.get('/api/v1/activity?count=20')
    expect(list.ok()).toBeTruthy()
    const items = await list.json()
    expect(items.find((a: { _id: string }) => a._id === id)).toBeTruthy()

    const put = await api.put('/api/v1/activity', {
      data: { _id: id, name: 'e2e-walk-updated', duration: 45 },
    })
    expect(put.ok()).toBeTruthy()
    const updated = await put.json()
    expect(updated.name).toBe('e2e-walk-updated')
    expect(updated.duration).toBe(45)

    const del = await api.delete(`/api/v1/activity/${id}`)
    expect(del.ok()).toBeTruthy()
  })
})
