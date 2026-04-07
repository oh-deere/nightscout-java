import { createHash, randomUUID } from 'node:crypto'
import { test as base, expect, type APIRequestContext } from '@playwright/test'

const API_SECRET = process.env.API_SECRET ?? 'Jq6RH1NjytCPjnV'

export const apiSecretHash = createHash('sha1').update(API_SECRET).digest('hex')

/**
 * Authenticated API request fixture — every request automatically carries the api-secret
 * header so individual tests don't have to set it.
 */
export const test = base.extend<{ api: APIRequestContext }>({
  api: async ({ playwright, baseURL }, use) => {
    const ctx = await playwright.request.newContext({
      baseURL,
      extraHTTPHeaders: {
        'api-secret': apiSecretHash,
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
    })
    await use(ctx)
    await ctx.dispose()
  },
})

export { expect }

/** Generate a unique sysTime so each test's entries don't collide on the unique constraint. */
export function uniqueSysTime(): string {
  return `e2e-${Date.now()}-${randomUUID()}`
}

/** Generate a unique created_at ISO timestamp for treatment dedup keys. */
export function uniqueCreatedAt(): string {
  // Add a small random offset so concurrent runs don't collide
  const ms = Date.now() + Math.floor(Math.random() * 100000)
  return new Date(ms).toISOString()
}
