import { useState } from 'react'
import {
  Box,
  Button,
  ButtonGroup,
  Divider,
  IconButton,
  InputAdornment,
  Popover,
  Slider,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import SettingsIcon from '@mui/icons-material/Settings'
import {
  useViewSettings,
  type BgRanges,
  type ChartHours,
  type TirHours,
} from '../hooks/useViewSettings'

const CHART_OPTIONS: ChartHours[] = [3, 6, 12, 24]
const TIR_OPTIONS: { hours: TirHours; label: string }[] = [
  { hours: 24, label: '24h' },
  { hours: 168, label: '7d' },
  { hours: 336, label: '14d' },
]

export function SettingsMenu() {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const settings = useViewSettings()

  return (
    <>
      <Tooltip title="View settings">
        <IconButton color="inherit" size="small" onClick={(e) => setAnchor(e.currentTarget)}>
          <SettingsIcon />
        </IconButton>
      </Tooltip>
      <Popover
        open={Boolean(anchor)}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { maxHeight: '85vh' } } }}
      >
        <Box sx={{ p: 2.5, width: 320 }}>
          <Stack spacing={2.5}>
            <Section label="Graph time range">
              <ButtonGroup size="small" variant="outlined" fullWidth>
                {CHART_OPTIONS.map((h) => (
                  <Button
                    key={h}
                    variant={h === settings.chartHours ? 'contained' : 'outlined'}
                    onClick={() => settings.setChartHours(h)}
                  >
                    {h}h
                  </Button>
                ))}
              </ButtonGroup>
            </Section>

            <Section label="Time in Range window">
              <ButtonGroup size="small" variant="outlined" fullWidth>
                {TIR_OPTIONS.map((o) => (
                  <Button
                    key={o.hours}
                    variant={o.hours === settings.tirHours ? 'contained' : 'outlined'}
                    onClick={() => settings.setTirHours(o.hours)}
                  >
                    {o.label}
                  </Button>
                ))}
              </ButtonGroup>
            </Section>

            <Divider />

            <Section label="Glucose ranges (mg/dL)">
              <RangeEditor ranges={settings.ranges} onChange={settings.setRanges} />
              <Button size="small" onClick={settings.resetRanges} sx={{ alignSelf: 'flex-end' }}>
                Reset to defaults
              </Button>
            </Section>

            <Divider />

            <Section label="Chart line">
              <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Typography variant="body2">Show connecting line</Typography>
                <Switch
                  size="small"
                  checked={settings.showLine}
                  onChange={(e) => settings.setShowLine(e.target.checked)}
                />
              </Stack>
              <Stack spacing={0.5} sx={{ opacity: settings.showLine ? 1 : 0.4 }}>
                <Typography variant="body2">Smoothing</Typography>
                <Slider
                  size="small"
                  value={settings.smoothing}
                  onChange={(_, v) => settings.setSmoothing(v as number)}
                  min={0}
                  max={3}
                  step={1}
                  marks={[
                    { value: 0, label: 'Raw' },
                    { value: 1, label: 'Light' },
                    { value: 2, label: 'Medium' },
                    { value: 3, label: 'Heavy' },
                  ]}
                  disabled={!settings.showLine}
                />
              </Stack>
            </Section>
          </Stack>
        </Box>
      </Popover>
    </>
  )
}

function Section({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <Stack spacing={1}>
      <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
        {label.toUpperCase()}
      </Typography>
      {children}
    </Stack>
  )
}

function RangeEditor({
  ranges,
  onChange,
}: {
  ranges: BgRanges
  onChange: (next: BgRanges) => void
}) {
  const update = (key: keyof BgRanges) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const v = parseInt(e.target.value, 10)
    if (Number.isNaN(v)) return
    onChange({ ...ranges, [key]: v })
  }

  return (
    <Stack spacing={1}>
      <RangeRow
        label="Urgent high"
        value={ranges.urgentHigh}
        onChange={update('urgentHigh')}
        color="#e53935"
      />
      <RangeRow
        label="Target high"
        value={ranges.targetHigh}
        onChange={update('targetHigh')}
        color="#ffb300"
      />
      <RangeRow
        label="Target low"
        value={ranges.targetLow}
        onChange={update('targetLow')}
        color="#ffb300"
      />
      <RangeRow
        label="Urgent low"
        value={ranges.urgentLow}
        onChange={update('urgentLow')}
        color="#e53935"
      />
    </Stack>
  )
}

function RangeRow({
  label,
  value,
  onChange,
  color,
}: {
  label: string
  value: number
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  color: string
}) {
  return (
    <Stack direction="row" alignItems="center" spacing={1.5}>
      <Box sx={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: color }} />
      <Typography variant="body2" sx={{ flex: 1 }}>
        {label}
      </Typography>
      <TextField
        size="small"
        type="number"
        value={value}
        onChange={onChange}
        sx={{ width: 110 }}
        slotProps={{
          input: {
            endAdornment: <InputAdornment position="end">mg/dL</InputAdornment>,
          },
          htmlInput: { min: 30, max: 600, style: { textAlign: 'right' } },
        }}
      />
    </Stack>
  )
}
