import { useMemo, useState } from 'react'
import { Box, ButtonGroup, Button, Stack, Typography } from '@mui/material'
import { CurrentBg } from './CurrentBg'
import { BgChart } from './BgChart'
import { PluginPills } from './PluginPills'
import { useEntries, useProperties, useStatus } from '../hooks/useNightscoutData'

const HOUR_OPTIONS = [3, 6, 12, 24] as const

export function Dashboard() {
  const [hours, setHours] = useState<(typeof HOUR_OPTIONS)[number]>(6)
  const status = useStatus()
  const entries = useEntries(hours)
  const properties = useProperties()

  const sortedEntries = useMemo(
    () => (entries.data ?? []).slice().sort((a, b) => b.date - a.date),
    [entries.data],
  )

  const current = sortedEntries[0] ?? null
  const previous = sortedEntries[1] ?? null

  if (!status.data) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">Loading…</Typography>
      </Box>
    )
  }

  return (
    <Stack spacing={2} sx={{ p: 2, height: '100%' }}>
      <CurrentBg current={current} previous={previous} settings={status.data.settings} />
      <PluginPills properties={properties.data} />
      <Stack direction="row" justifyContent="center">
        <ButtonGroup size="small" variant="outlined">
          {HOUR_OPTIONS.map((h) => (
            <Button
              key={h}
              variant={h === hours ? 'contained' : 'outlined'}
              onClick={() => setHours(h)}
            >
              {h}h
            </Button>
          ))}
        </ButtonGroup>
      </Stack>
      <Box sx={{ flex: 1, minHeight: 280 }}>
        <BgChart entries={sortedEntries} settings={status.data.settings} hours={hours} />
      </Box>
    </Stack>
  )
}
