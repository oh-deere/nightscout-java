import { useMemo } from 'react'
import { Box, Card, Stack, Tooltip, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { NightscoutSettings } from '../types/nightscout'
import {
  bucketEntries,
  estimatedA1c,
  meanGlucose,
  standardDeviation,
  toPercents,
} from '../utils/tir'
import { formatBg } from '../utils/units'
import { useViewSettings } from '../hooks/useViewSettings'

interface Props {
  settings: NightscoutSettings
}

const COLORS = {
  urgentLow: '#c62828',
  low: '#ef6c00',
  inRange: '#2e7d32',
  high: '#ef6c00',
  urgentHigh: '#c62828',
}

export function TimeInRange({ settings }: Props) {
  const view = useViewSettings()
  const hours = view.tirHours

  const query = useQuery({
    queryKey: ['entries-tir', hours],
    queryFn: () => api.entriesSince(Date.now() - hours * 3600_000),
    staleTime: 60_000,
    refetchInterval: 5 * 60_000,
  })

  const windowLabel = hours === 24 ? '24h' : hours === 168 ? '7d' : `${Math.round(hours / 24)}d`

  const stats = useMemo(() => {
    const entries = query.data ?? []
    const buckets = bucketEntries(entries, settings.thresholds)
    return {
      buckets,
      percents: toPercents(buckets),
      mean: meanGlucose(entries),
      sd: standardDeviation(entries),
    }
  }, [query.data, settings.thresholds])

  const a1c = stats.mean > 0 ? estimatedA1c(stats.mean) : 0
  const cv = stats.mean > 0 ? (stats.sd / stats.mean) * 100 : 0

  return (
    <Card sx={{ p: 2 }}>
      <Stack spacing={1.5}>
        <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>
          Time in Range · {windowLabel}
        </Typography>

        {stats.buckets.total === 0 ? (
          <Typography color="text.secondary" variant="body2">
            No data
          </Typography>
        ) : (
          <>
            <Box sx={{ display: 'flex', height: 24, borderRadius: 1, overflow: 'hidden' }}>
              {(
                [
                  ['urgentLow', stats.percents.urgentLow],
                  ['low', stats.percents.low],
                  ['inRange', stats.percents.inRange],
                  ['high', stats.percents.high],
                  ['urgentHigh', stats.percents.urgentHigh],
                ] as const
              ).map(([key, pct]) =>
                pct > 0 ? (
                  <Tooltip key={key} title={`${labelOf(key)}: ${pct.toFixed(1)}%`}>
                    <Box
                      sx={{
                        width: `${pct}%`,
                        backgroundColor: COLORS[key],
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontSize: 11,
                        fontWeight: 600,
                      }}
                    >
                      {pct >= 8 ? `${pct.toFixed(0)}%` : ''}
                    </Box>
                  </Tooltip>
                ) : null,
              )}
            </Box>

            <Stack
              direction="row"
              spacing={3}
              flexWrap="wrap"
              useFlexGap
              sx={{ color: 'text.secondary', fontSize: 13 }}
            >
              <Stat label="In range" value={`${stats.percents.inRange.toFixed(0)}%`} />
              <Stat label="Mean" value={formatBg(stats.mean, settings.units)} />
              <Stat label="eA1C" value={`${a1c.toFixed(1)}%`} />
              <Stat label="CV" value={`${cv.toFixed(0)}%`} />
              <Stat label="readings" value={String(stats.buckets.total)} />
            </Stack>
          </>
        )}
      </Stack>
    </Card>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" spacing={0.5} alignItems="baseline">
      <Typography component="span" sx={{ color: 'text.secondary', fontSize: 12 }}>
        {label}
      </Typography>
      <Typography component="span" sx={{ color: 'text.primary', fontWeight: 600 }}>
        {value}
      </Typography>
    </Stack>
  )
}

function labelOf(key: keyof typeof COLORS): string {
  switch (key) {
    case 'urgentLow':
      return 'Urgent low'
    case 'low':
      return 'Low'
    case 'inRange':
      return 'In range'
    case 'high':
      return 'High'
    case 'urgentHigh':
      return 'Urgent high'
  }
}
