import { useMemo } from 'react'
import { Box, Stack, Typography } from '@mui/material'
import { BgHeader } from './BgHeader'
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
import { useViewSettings } from '../hooks/useViewSettings'
import { useEffectiveSettings } from '../hooks/useEffectiveSettings'

interface Props {
  notificationsEnabled: boolean
}

export function Dashboard({ notificationsEnabled }: Props) {
  const view = useViewSettings()
  const status = useStatus()
  const entries = useEntries(view.chartHours)
  const treatments = useTreatments(view.chartHours)
  const properties = useProperties()

  const sortedEntries = useMemo(
    () => (entries.data ?? []).slice().sort((a, b) => b.date - a.date),
    [entries.data],
  )

  const current = sortedEntries[0] ?? null
  const previous = sortedEntries[1] ?? null

  // Hooks must run unconditionally — pass an empty fallback when status is loading.
  const effective = useEffectiveSettings(
    status.data?.settings ?? {
      units: 'mg/dl',
      timeFormat: 24,
      theme: 'default',
      language: 'en',
      customTitle: '',
      showPlugins: '',
      enable: [],
      thresholds: { bgHigh: 260, bgTargetTop: 180, bgTargetBottom: 70, bgLow: 55 },
      alarmTypes: [],
      authDefaultRoles: 'denied',
      nightMode: false,
    },
  )

  useAlarmNotifier(current, effective, notificationsEnabled)

  if (!status.data) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">Loading…</Typography>
      </Box>
    )
  }

  return (
    <Stack spacing={2} sx={{ p: 2 }}>
      <BgHeader
        current={current}
        previous={previous}
        settings={effective}
        properties={properties.data}
      />
      <PluginPills properties={properties.data} />
      <Box sx={{ height: { xs: 360, sm: 520 }, width: '100%' }}>
        <BgChart
          entries={sortedEntries}
          treatments={treatments.data}
          settings={effective}
          hours={view.chartHours}
          showLine={view.showLine}
          smoothing={view.smoothing}
        />
      </Box>
      <SensorCard properties={properties.data} />
      <TimeInRange settings={effective} />
    </Stack>
  )
}
