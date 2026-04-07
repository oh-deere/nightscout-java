import { useMemo, useState } from 'react'
import { Box, ButtonGroup, Button, Stack, Typography } from '@mui/material'
import { CurrentBg } from './CurrentBg'
import { BgChart } from './BgChart'
import { PluginPills } from './PluginPills'
import { TimeInRange } from './TimeInRange'
import { SensorCard } from './SensorCard'
import {
  useEntries,
  useProperties,
  useStatus,
  useTreatments,
} from '../hooks/useNightscoutData'
import { useAlarmNotifier } from '../hooks/useNotifications'

const HOUR_OPTIONS = [3, 6, 12, 24] as const

interface Props {
  notificationsEnabled: boolean
}

export function Dashboard({ notificationsEnabled }: Props) {
  const [hours, setHours] = useState<(typeof HOUR_OPTIONS)[number]>(6)
  const status = useStatus()
  const entries = useEntries(hours)
  const treatments = useTreatments(hours)
  const properties = useProperties()

  const sortedEntries = useMemo(
    () => (entries.data ?? []).slice().sort((a, b) => b.date - a.date),
    [entries.data],
  )

  const current = sortedEntries[0] ?? null
  const previous = sortedEntries[1] ?? null

  useAlarmNotifier(current, status.data?.settings, notificationsEnabled)

  if (!status.data) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">Loading…</Typography>
      </Box>
    )
  }

  return (
    <Stack spacing={2} sx={{ p: 2 }}>
      <CurrentBg current={current} previous={previous} settings={status.data.settings} />
      <PluginPills properties={properties.data} />
      <SensorCard properties={properties.data} />
      <TimeInRange settings={status.data.settings} />
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
      <Box sx={{ height: { xs: 300, sm: 400 }, width: '100%' }}>
        <BgChart
          entries={sortedEntries}
          treatments={treatments.data}
          settings={status.data.settings}
          hours={hours}
        />
      </Box>
    </Stack>
  )
}
