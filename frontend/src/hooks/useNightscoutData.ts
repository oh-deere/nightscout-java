import { useQuery } from '@tanstack/react-query'
import { api, getApiSecretHash } from '../api/client'

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

export function useTreatments(hours = 6) {
  return useQuery({
    queryKey: ['treatments', hours],
    queryFn: () => api.treatmentsSince(new Date(Date.now() - hours * 3600_000).toISOString()),
    refetchInterval: POLL_INTERVAL_MS,
  })
}

export function useVerifyAuth() {
  return useQuery({
    queryKey: ['verifyauth'],
    queryFn: api.verifyAuth,
    staleTime: 5 * 60_000,
    retry: false,
    enabled: getApiSecretHash() != null,
  })
}

export function useProperties(agpDays = 14) {
  const offsetMinutes = -new Date().getTimezoneOffset()
  return useQuery({
    queryKey: ['properties', offsetMinutes, agpDays],
    queryFn: () => api.properties(offsetMinutes, agpDays),
    refetchInterval: POLL_INTERVAL_MS,
  })
}

/**
 * Ambulatory Glucose Profile bands. Re-fetches every five minutes since the
 * underlying SGV cadence is 5 min and the trailing window changes slowly.
 * The browser's UTC offset is forwarded so the buckets land on the user's
 * local clock without any backend tz config.
 */
export function useAgp(days = 14, bucketMinutes = 15) {
  const offsetMinutes = -new Date().getTimezoneOffset()
  return useQuery({
    queryKey: ['agp', days, bucketMinutes, offsetMinutes],
    queryFn: () => api.agp(days, bucketMinutes, offsetMinutes),
    refetchInterval: 5 * 60_000,
    staleTime: 5 * 60_000,
    enabled: getApiSecretHash() != null,
  })
}
