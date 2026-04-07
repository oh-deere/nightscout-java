import { test, expect } from './helpers'

const sampleProfile = {
  defaultProfile: 'E2E',
  store: {
    E2E: {
      dia: 4,
      carbratio: [{ time: '00:00', value: 12 }],
      sens: [{ time: '00:00', value: 60 }],
      basal: [{ time: '00:00', value: 0.9 }],
      target_low: [{ time: '00:00', value: 80 }],
      target_high: [{ time: '00:00', value: 160 }],
      units: 'mg/dl',
      timezone: 'Europe/Stockholm',
    },
  },
}

test.describe('API v1 — profile', () => {
  test('POST → GET /profile → GET /profiles alias → GET /profile/current → PUT', async ({ api }) => {
    // CREATE
    const post = await api.post('/api/v1/profile.json', { data: sampleProfile })
    expect(post.ok()).toBeTruthy()
    const created = await post.json()
    expect(created._id).toBeTruthy()
    expect(created.defaultProfile).toBe('E2E')
    const id = created._id

    // GET /profile
    const list = await api.get('/api/v1/profile')
    expect(list.ok()).toBeTruthy()
    const profiles = await list.json()
    expect(profiles.find((p: { _id: string }) => p._id === id)).toBeTruthy()

    // GET /profiles alias
    const aliasList = await api.get('/api/v1/profiles')
    expect(aliasList.ok()).toBeTruthy()
    const aliasItems = await aliasList.json()
    expect(aliasItems.find((p: { _id: string }) => p._id === id)).toBeTruthy()

    // GET /profile/current — should be the most recent
    const current = await api.get('/api/v1/profile/current')
    expect(current.ok()).toBeTruthy()
    const currentBody = await current.json()
    expect(currentBody._id).toBe(id)
    expect(currentBody.store).toBeTruthy()

    // PUT — update defaultProfile and basal
    const updated = {
      _id: id,
      defaultProfile: 'E2E-updated',
      store: {
        'E2E-updated': {
          ...sampleProfile.store.E2E,
          basal: [{ time: '00:00', value: 1.2 }],
        },
      },
    }
    const put = await api.put('/api/v1/profile.json', { data: updated })
    expect(put.ok()).toBeTruthy()
    const putBody = await put.json()
    expect(putBody.defaultProfile).toBe('E2E-updated')
  })

  test('PUT without _id returns 400', async ({ api }) => {
    const res = await api.put('/api/v1/profile.json', { data: { defaultProfile: 'orphan' } })
    expect(res.status()).toBe(400)
  })

  test('PUT with non-existent id returns 404', async ({ api }) => {
    const res = await api.put('/api/v1/profile.json', {
      data: { _id: '00000000-0000-0000-0000-000000000000', defaultProfile: 'ghost' },
    })
    expect(res.status()).toBe(404)
  })
})
