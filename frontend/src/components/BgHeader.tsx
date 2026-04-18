import { useEffect, useState } from 'react'
import { Box, Stack, Typography } from '@mui/material'
import type { Entry, NightscoutSettings, PluginProperties } from '../types/nightscout'
import { DIRECTION_ARROW, formatBg, formatDelta } from '../utils/units'
import { bgColor } from '../theme/theme'
import { formatDistanceToNowStrict } from 'date-fns'

interface Props {
  current: Entry | null
  previous: Entry | null
  settings: NightscoutSettings
  properties: PluginProperties | undefined
}

function useClock() {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 30_000)
    return () => clearInterval(id)
  }, [])
  return now
}

function formatTime(d: Date): string {
  return d.toLocaleTimeString('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

function formatDate(d: Date): string {
  return d.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })
}

export function BgHeader({ current, previous, settings, properties }: Props) {
  const now = useClock()

  if (!current || current.sgv == null) {
    return (
      <Box sx={{ textAlign: 'center', py: 3, px: 1 }}>
        <Typography variant="h2" color="text.secondary">
          ---
        </Typography>
        <Typography variant="body2" color="text.secondary">
          No data
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ mt: 1, display: 'block' }}>
          {formatTime(now)} · {formatDate(now)}
        </Typography>
      </Box>
    )
  }

  const sgv = current.sgv
  const color = bgColor(sgv, settings.thresholds)
  const arrow = current.direction ? (DIRECTION_ARROW[current.direction] ?? '') : ''
  const delta = previous?.sgv != null ? sgv - previous.sgv : null
  const ageMins = Math.round((now.getTime() - current.date) / 60000)
  const stale = ageMins > 15

  const iobValue = properties?.iob?.data?.iob as number | undefined
  const cobValue = properties?.cob?.data?.displayCob as number | undefined

  const clock = (
    <Stack
      direction={{ xs: 'row', sm: 'column' }}
      spacing={{ xs: 1, sm: 0.25 }}
      alignItems={{ xs: 'baseline', sm: 'flex-start' }}
      justifyContent={{ xs: 'center', sm: 'flex-start' }}
      sx={{ width: { xs: '100%', sm: 'auto' }, flex: { sm: 1 }, minWidth: 0 }}
    >
      <Typography
        sx={{
          fontSize: { xs: '1.75rem', sm: '4.5rem' },
          fontWeight: 700,
          color: 'text.secondary',
          fontVariantNumeric: 'tabular-nums',
          lineHeight: 0.9,
        }}
      >
        {formatTime(now)}
      </Typography>
      <Typography sx={{ color: 'text.disabled', fontSize: { xs: 12, sm: 14 } }}>
        {formatDate(now)}
      </Typography>
    </Stack>
  )

  const center = (
    <Stack alignItems="center" spacing={0.5} sx={{ width: '100%', flex: { sm: 2 }, minWidth: 0 }}>
      <Stack direction="row" alignItems="baseline" spacing={1.5}>
        <Typography
          sx={{
            fontSize: { xs: '4rem', sm: '6.5rem' },
            fontWeight: 700,
            lineHeight: 0.9,
            color,
            opacity: stale ? 0.5 : 1,
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          {formatBg(sgv, settings.units)}
        </Typography>
        <Typography sx={{ fontSize: { xs: '2.25rem', sm: '3.5rem' }, color, lineHeight: 0.9 }}>
          {arrow}
        </Typography>
      </Stack>
      <Stack
        direction="row"
        spacing={1.5}
        useFlexGap
        flexWrap="wrap"
        justifyContent="center"
        sx={{ color: 'text.secondary', fontSize: { xs: 13, sm: 14 }, rowGap: 0.5 }}
      >
        {delta != null && (
          <Typography variant="body2">{formatDelta(delta, settings.units)}</Typography>
        )}
        <Typography variant="body2">
          {formatDistanceToNowStrict(current.date, { addSuffix: true })}
        </Typography>
        <Typography variant="body2">{settings.units}</Typography>
        {properties?.agpRank && (
          <Typography variant="body2" sx={{ color: 'primary.light' }}>
            p{properties.agpRank.percentile}
          </Typography>
        )}
      </Stack>
    </Stack>
  )

  const metrics = (
    <Stack
      direction={{ xs: 'row', sm: 'column' }}
      spacing={{ xs: 4, sm: 0.25 }}
      justifyContent={{ xs: 'center', sm: 'flex-end' }}
      alignItems={{ xs: 'baseline', sm: 'flex-end' }}
      sx={{ width: { xs: '100%', sm: 'auto' }, flex: { sm: 1 }, minWidth: 0 }}
    >
      <Metric label="IOB" value={iobValue != null ? iobValue.toFixed(2) + ' U' : '—'} />
      <Metric label="COB" value={cobValue != null ? cobValue + ' g' : '—'} />
    </Stack>
  )

  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      alignItems="center"
      justifyContent={{ xs: 'flex-start', sm: 'space-between' }}
      spacing={{ xs: 1, sm: 1 }}
      sx={{ py: { xs: 1.5, sm: 2 }, px: 1 }}
    >
      {/* xs order: BG hero first, then clock, then metrics. sm+: clock | BG | metrics. */}
      <Box sx={{ order: { xs: 1, sm: 2 }, width: '100%', flex: { sm: 2 }, minWidth: 0 }}>
        {center}
      </Box>
      <Box sx={{ order: { xs: 2, sm: 1 }, width: '100%', flex: { sm: 1 }, minWidth: 0 }}>
        {clock}
      </Box>
      <Box sx={{ order: 3, width: '100%', flex: { sm: 1 }, minWidth: 0 }}>{metrics}</Box>
    </Stack>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <Stack
      direction={{ xs: 'row', sm: 'column' }}
      spacing={{ xs: 0.75, sm: -0.5 }}
      alignItems={{ xs: 'baseline', sm: 'flex-end' }}
    >
      <Typography
        component="span"
        sx={{ color: 'text.disabled', fontSize: { xs: 11, sm: 14 }, fontWeight: 500 }}
      >
        {label}
      </Typography>
      <Typography
        component="span"
        sx={{
          color: 'text.primary',
          fontSize: { xs: '1.4rem', sm: '2.8rem' },
          fontWeight: 700,
          fontVariantNumeric: 'tabular-nums',
          lineHeight: 1,
          whiteSpace: 'nowrap',
        }}
      >
        {value}
      </Typography>
    </Stack>
  )
}
