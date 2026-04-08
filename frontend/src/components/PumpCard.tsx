import { Box, Card, CardContent, Chip, Stack, Tooltip, Typography } from '@mui/material'
import BatteryFullIcon from '@mui/icons-material/BatteryFull'
import OpacityIcon from '@mui/icons-material/Opacity'
import LoopIcon from '@mui/icons-material/Loop'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import type { PluginProperties, PumpData } from '../types/nightscout'

interface Props {
  properties?: PluginProperties
}

export function PumpCard({ properties }: Props) {
  const data = properties?.pump?.data as PumpData | undefined
  if (!data) return null

  const hasAnything =
    data.pumpReservoir != null ||
    data.pumpBatteryPercent != null ||
    data.pumpBatteryVoltage != null ||
    data.loopIob != null ||
    data.loopCob != null ||
    data.loopTimestamp != null
  if (!hasAnything) return null

  const sourceLabel =
    data.source === 'loop'
      ? `${data.loopName ?? 'Loop'}${data.loopVersion ? ` v${data.loopVersion}` : ''}`
      : data.source === 'openaps'
        ? 'OpenAPS / AAPS'
        : 'Pump'

  return (
    <Card variant="outlined">
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mb: 1 }}>
          <LoopIcon fontSize="small" color="action" />
          <Typography variant="subtitle2" sx={{ flexGrow: 1, fontWeight: 600 }}>
            {sourceLabel}
          </Typography>
          {data.loopStale && (
            <Tooltip title={`Last loop ${data.loopAgeMinutes ?? '?'} min ago`}>
              <Chip
                size="small"
                color="warning"
                icon={<WarningAmberIcon />}
                label="Stale loop"
              />
            </Tooltip>
          )}
          {!data.loopStale && data.loopAgeMinutes != null && (
            <Typography variant="caption" color="text.secondary">
              {data.loopAgeMinutes} min ago
            </Typography>
          )}
        </Stack>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(4, 1fr)' },
            gap: 1.5,
          }}
        >
          {data.loopIob != null && <Metric label="IOB" value={`${data.loopIob.toFixed(2)} U`} />}
          {data.loopCob != null && <Metric label="COB" value={`${data.loopCob.toFixed(0)} g`} />}
          {data.pumpReservoir != null && (
            <Metric
              label="Reservoir"
              value={`${data.pumpReservoir.toFixed(1)} U`}
              icon={<OpacityIcon fontSize="small" />}
            />
          )}
          {data.pumpBatteryPercent != null && (
            <Metric
              label="Battery"
              value={`${data.pumpBatteryPercent}%`}
              icon={<BatteryFullIcon fontSize="small" />}
            />
          )}
          {data.pumpBatteryPercent == null && data.pumpBatteryVoltage != null && (
            <Metric
              label="Battery"
              value={`${data.pumpBatteryVoltage.toFixed(2)} V`}
              icon={<BatteryFullIcon fontSize="small" />}
            />
          )}
          {data.openapsEventualBg != null && (
            <Metric label="Eventual BG" value={`${data.openapsEventualBg}`} />
          )}
        </Box>

        {data.openapsReason && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            {data.openapsReason}
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}

function Metric({
  label,
  value,
  icon,
}: {
  label: string
  value: string
  icon?: React.ReactNode
}) {
  return (
    <Stack spacing={0.25}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Stack direction="row" spacing={0.5} alignItems="center">
        {icon}
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {value}
        </Typography>
      </Stack>
    </Stack>
  )
}
