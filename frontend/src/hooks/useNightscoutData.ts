import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

const POLL_INTERVAL_MS = 60_000 // 1 minute

export function useStatus() {
  return useQuery({
    queryKey: ['status'],
    queryFn: api.status,
    staleTime: 5 * 60_000,
  })
}

export function useEntries(hours = 6) {
  return useQuery({
    queryKey: ['entries', hours],
    queryFn: () => api.entriesSince(Date.now() - hours * 3600_000),
    refetchInterval: POLL_INTERVAL_MS,
    refetchOnWindowFocus: true,
  })
}

export function useProperties() {
  return useQuery({
    queryKey: ['properties'],
    queryFn: api.properties,
    refetchInterval: POLL_INTERVAL_MS,
  })
}
