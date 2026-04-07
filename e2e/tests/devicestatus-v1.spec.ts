import { test, expect } from './helpers'

test.describe('API v1 — devicestatus', () => {
  test('POST → GET → DELETE round-trip', async ({ api }) => {
    const post = await api.post('/api/v1/devicestatus.json', {
      data: [
        {
          device: 'e2e-test',
          uploader: { battery: 88 },
          pump: { battery: { percent: 75 }, reservoir: 100 },
        },
      ],
    })
    expect(post.ok()).toBeTruthy()

    const list = await api.get('/api/v1/devicestatus.json?count=10')
    expect(list.ok()).toBeTruthy()
    const items = await list.json()
    const recent = items.find((d: { device: string }) => d.device === 'e2e-test')
    expect(recent).toBeTruthy()
    expect(recent.uploader.battery).toBe(88)

    // Cleanup
    if (recent._id) {
      const del = await api.delete(`/api/v1/devicestatus/${recent._id}`)
      expect(del.ok()).toBeTruthy()
    }
  })
})
