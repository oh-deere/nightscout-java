import { defineConfig } from '@playwright/test'

const baseURL = process.env.BASE_URL ?? 'http://localhost:8090'
const apiSecret = process.env.API_SECRET ?? 'Jq6RH1NjytCPjnV'

export default defineConfig({
  testDir: './tests',
  fullyParallel: false, // tests share a single database — keep them sequential
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  workers: 1,
  use: {
    baseURL,
    extraHTTPHeaders: {
      // Will be overridden per request when needed
      Accept: 'application/json',
    },
  },
  metadata: {
    apiSecret,
  },
})
