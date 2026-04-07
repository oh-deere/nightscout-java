import { Card, LinearProgress, Stack, Typography } from '@mui/material'
import SensorsIcon from '@mui/icons-material/Sensors'
import type { PluginProperties } from '../types/nightscout'

interface Props {
  properties: PluginProperties | undefined
  // Sensor lifetime in hours, defaults to 14 days for FreeStyle Libre
  sensorLifeHours?: number
}

export function SensorCard({ properties, sensorLifeHours = 14 * 24 }: Props) {
  const sage = properties?.sage
  if (!sage || sage.value === 'N/A' || !sage.data) return null

  const hours = (sage.data.hours as number | undefined) ?? 0
  const remainingHours = Math.max(0, sensorLifeHours - hours)
  const remainingDays = Math.floor(remainingHours / 24)
  const remainingHoursPart = Math.round(remainingHours % 24)
  const pctUsed = Math.min(100, (hours / sensorLifeHours) * 100)
  const level = (sage.data.level as string | undefined) ?? sage.level ?? 'ok'

  const color: 'success' | 'warning' | 'error' =
    remainingHours <= 12 || level === 'urgent'
      ? 'error'
      : remainingHours <= 48 || level === 'warn'
        ? 'warning'
        : 'success'

  const remainingLabel =
    remainingHours <= 0
      ? 'Sensor expired'
      : remainingDays > 0
        ? `${remainingDays}d ${remainingHoursPart}h remaining`
        : `${remainingHoursPart}h remaining`

  return (
    <Card sx={{ p: 2 }}>
      <Stack direction="row" alignItems="center" spacing={2}>
        <SensorsIcon
          color={color}
          sx={{ fontSize: 32 }}
        />
        <Stack sx={{ flex: 1, minWidth: 0 }} spacing={0.5}>
          <Stack
            direction="row"
            alignItems="baseline"
            justifyContent="space-between"
            spacing={1}
          >
            <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>
              Sensor
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              {remainingLabel}
            </Typography>
          </Stack>
          <LinearProgress
            variant="determinate"
            value={pctUsed}
            color={color}
            sx={{ height: 6, borderRadius: 3 }}
          />
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            Age: {sage.value}
          </Typography>
        </Stack>
      </Stack>
    </Card>
  )
}
