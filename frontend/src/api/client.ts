import type {
  Entry,
  NightscoutStatus,
  PluginProperties,
  Treatment,
} from '../types/nightscout'

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
  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

async function requestJson<T>(path: string, method: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
}

export interface AdminApiKey {
  id: string
  name: string
  scope: 'read' | 'write' | 'admin'
  createdAt: string
  createdBy: string
  lastUsedAt: string | null
  expiresAt: string | null
  enabled: boolean
}

export interface AdminApiKeyCreated extends AdminApiKey {
  token: string
}

export interface AdminSetting {
  key: string
  value: string | null
  updatedAt: string
  updatedBy: string
}

export interface AlarmHistoryEntry {
  id: string
  occurredAt: string
  type: string
  level: number
  title: string
  message: string | null
}

export interface AdminAuditEntry {
  id: string
  occurredAt: string
  actorSubject: string
  actorKind: string
  action: string
  target: string
  beforeValue: { value: string } | null
  afterValue: { value: string } | null
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
    request<Entry[]>(
      `/api/v1/entries.json?${encodeURIComponent('find[date][$gte]')}=${sinceMs}&count=1000`,
    ),
  current: () => request<Entry[]>('/api/v1/entries/current.json'),
  treatmentsSince: (sinceIso: string) =>
    request<Treatment[]>(
      `/api/v1/treatments.json?${encodeURIComponent('find[created_at][$gte]')}=${encodeURIComponent(
        sinceIso,
      )}&count=1000`,
    ),
  properties: () => request<PluginProperties>('/api/v1/properties'),
  alarmHistory: (limit = 50) =>
    request<AlarmHistoryEntry[]>(`/api/v1/alarms/history?limit=${limit}`),
  verifyAuth: () =>
    request<{
      message: string
      status: number
      sub?: string
      permissions?: string[]
      admin?: boolean
    }>('/api/v1/verifyauth'),

  admin: {
    listKeys: () => request<AdminApiKey[]>('/api/v2/admin/keys'),
    createKey: (name: string, scope: AdminApiKey['scope'], expiresAt?: string) =>
      requestJson<AdminApiKeyCreated>('/api/v2/admin/keys', 'POST', { name, scope, expiresAt }),
    revokeKey: (id: string) => requestJson<void>(`/api/v2/admin/keys/${id}`, 'DELETE'),

    listSettings: () => request<AdminSetting[]>('/api/v2/admin/settings'),
    putSetting: (key: string, value: unknown) =>
      requestJson<AdminSetting>(`/api/v2/admin/settings/${encodeURIComponent(key)}`, 'PUT', value),
    deleteSetting: (key: string) =>
      requestJson<void>(`/api/v2/admin/settings/${encodeURIComponent(key)}`, 'DELETE'),

    audit: (limit = 100) => request<AdminAuditEntry[]>(`/api/v2/admin/audit?limit=${limit}`),
  },
}
