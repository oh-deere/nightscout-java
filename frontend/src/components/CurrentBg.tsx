import { Box, Stack, Typography } from '@mui/material'
import type { Entry, NightscoutSettings } from '../types/nightscout'
import { DIRECTION_ARROW, formatBg, formatDelta } from '../utils/units'
import { bgColor } from '../theme/theme'
import { formatDistanceToNowStrict } from 'date-fns'
import { useNowMs } from '../hooks/useNowMs'

interface Props {
  current: Entry | null
  previous: Entry | null
  settings: NightscoutSettings
}

export function CurrentBg({ current, previous, settings }: Props) {
  const nowMs = useNowMs()
  if (!current || current.sgv == null) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <Typography variant="h2" color="text.secondary">
          ---
        </Typography>
        <Typography variant="body2" color="text.secondary">
          No data
        </Typography>
      </Box>
    )
  }

  const sgv = current.sgv
  const color = bgColor(sgv, settings.thresholds)
  const arrow = current.direction ? (DIRECTION_ARROW[current.direction] ?? '') : ''
  const delta = previous?.sgv != null ? sgv - previous.sgv : null
  const ageMins = Math.round((nowMs - current.date) / 60000)
  const stale = ageMins > 15

  return (
    <Stack alignItems="center" spacing={1} sx={{ py: 3 }}>
      <Stack direction="row" alignItems="baseline" spacing={2}>
        <Typography
          sx={{
            fontSize: { xs: '5rem', sm: '7rem' },
            fontWeight: 700,
            lineHeight: 1,
            color,
            opacity: stale ? 0.5 : 1,
          }}
        >
          {formatBg(sgv, settings.units)}
        </Typography>
        <Typography sx={{ fontSize: { xs: '3rem', sm: '4rem' }, color, lineHeight: 1 }}>
          {arrow}
        </Typography>
      </Stack>
      <Stack direction="row" spacing={3} sx={{ color: 'text.secondary' }}>
        {delta != null && (
          <Typography variant="body1">{formatDelta(delta, settings.units)}</Typography>
        )}
        <Typography variant="body1">
          {formatDistanceToNowStrict(current.date, { addSuffix: true })}
        </Typography>
        <Typography variant="body1">{settings.units}</Typography>
      </Stack>
    </Stack>
  )
}
