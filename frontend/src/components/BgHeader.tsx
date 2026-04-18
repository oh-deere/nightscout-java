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
  // Always 24h format regardless of locale
  return d.toLocaleTimeString('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

export function BgHeader({ current, previous, settings, properties }: Props) {
  const now = useClock()

  if (!current || current.sgv == null) {
    return (
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{ py: 3, px: 1 }}
      >
        <SideColumn align="left">
          <Typography
            sx={{
              fontSize: { xs: '3rem', sm: '4.5rem' },
              fontWeight: 700,
              color: 'text.secondary',
              fontVariantNumeric: 'tabular-nums',
              lineHeight: 0.9,
            }}
          >
            {formatTime(now)}
          </Typography>
        </SideColumn>
        <Box sx={{ flex: 2, textAlign: 'center' }}>
          <Typography variant="h2" color="text.secondary">
            ---
          </Typography>
          <Typography variant="body2" color="text.secondary">
            No data
          </Typography>
        </Box>
        <SideColumn align="right" />
      </Stack>
    )
  }

  const sgv = current.sgv
  const color = bgColor(sgv, settings.thresholds)
  const arrow = current.direction ? (DIRECTION_ARROW[current.direction] ?? '') : ''
  const delta = previous?.sgv != null ? sgv - previous.sgv : null
  const ageMins = Math.round((Date.now() - current.date) / 60000)
  const stale = ageMins > 15

  const iobValue = properties?.iob?.data?.iob as number | undefined
  const cobValue = properties?.cob?.data?.displayCob as number | undefined

  return (
    <Stack
      direction="row"
      alignItems="center"
      justifyContent="space-between"
      sx={{ py: 2, px: 1, gap: 1 }}
    >
      {/* Left: clock */}
      <SideColumn align="left">
        <Typography
          sx={{
            fontSize: { xs: '3rem', sm: '4.5rem' },
            fontWeight: 700,
            color: 'text.secondary',
            fontVariantNumeric: 'tabular-nums',
            lineHeight: 0.9,
          }}
        >
          {formatTime(now)}
        </Typography>
        <Typography sx={{ color: 'text.disabled', fontSize: { xs: 12, sm: 14 } }}>
          {now.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })}
        </Typography>
      </SideColumn>

      {/* Center: BG + arrow + delta + age */}
      <Stack alignItems="center" spacing={0.5} sx={{ flex: 2, minWidth: 0 }}>
        <Stack direction="row" alignItems="baseline" spacing={1.5}>
          <Typography
            sx={{
              fontSize: { xs: '4.5rem', sm: '6.5rem' },
              fontWeight: 700,
              lineHeight: 0.9,
              color,
              opacity: stale ? 0.5 : 1,
            }}
          >
            {formatBg(sgv, settings.units)}
          </Typography>
          <Typography sx={{ fontSize: { xs: '2.5rem', sm: '3.5rem' }, color, lineHeight: 0.9 }}>
            {arrow}
          </Typography>
        </Stack>
        <Stack
          direction="row"
          spacing={2}
          sx={{ color: 'text.secondary', fontSize: { xs: 13, sm: 14 } }}
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

      {/* Right: IOB / COB */}
      <SideColumn align="right">
        <Metric label="IOB" value={iobValue != null ? iobValue.toFixed(2) + ' U' : '—'} />
        <Metric label="COB" value={cobValue != null ? cobValue + ' g' : '—'} />
      </SideColumn>
    </Stack>
  )
}

function SideColumn({ align, children }: { align: 'left' | 'right'; children?: React.ReactNode }) {
  return (
    <Stack
      sx={{
        flex: 1,
        minWidth: 0,
        alignItems: align === 'left' ? 'flex-start' : 'flex-end',
      }}
      spacing={0.25}
    >
      {children}
    </Stack>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <Stack alignItems="flex-end" spacing={-0.5}>
      <Typography
        component="span"
        sx={{ color: 'text.disabled', fontSize: { xs: 12, sm: 14 }, fontWeight: 500 }}
      >
        {label}
      </Typography>
      <Typography
        component="span"
        sx={{
          color: 'text.primary',
          fontSize: { xs: '2rem', sm: '2.8rem' },
          fontWeight: 700,
          fontVariantNumeric: 'tabular-nums',
          lineHeight: 1,
        }}
      >
        {value}
      </Typography>
    </Stack>
  )
}
