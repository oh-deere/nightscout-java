import type { Entry, NightscoutStatus, PluginProperties } from '../types/nightscout'

const SECRET_KEY = 'nightscout.apiSecretHash'

export function getApiSecretHash(): string | null {
  return localStorage.getItem(SECRET_KEY)
}

export function setApiSecretHash(hash: string): void {
  localStorage.setItem(SECRET_KEY, hash)
}

export function clearApiSecretHash(): void {
  localStorage.removeItem(SECRET_KEY)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const hash = getApiSecretHash()
  if (hash) {
    headers.set('api-secret', hash)
  }
  headers.set('Accept', 'application/json')
  const res = await fetch(path, { ...init, headers })
  if (!res.ok) {
    if (res.status === 401) {
      throw new ApiError('Unauthorized', 401)
    }
    throw new ApiError(`HTTP ${res.status}`, res.status)
  }
  return res.json() as Promise<T>
}

export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

export const api = {
  status: () => request<NightscoutStatus>('/api/v1/status.json'),
  entries: (count = 288) => request<Entry[]>(`/api/v1/entries.json?count=${count}`),
  entriesSince: (sinceMs: number) =>
    request<Entry[]>(`/api/v1/entries.json?find[date][$gte]=${sinceMs}&count=1000`),
  current: () => request<Entry[]>('/api/v1/entries/current.json'),
  properties: () => request<PluginProperties>('/api/v1/properties'),
  verifyAuth: () => request<{ message: string; status: number }>('/api/v1/verifyauth'),
}
