import { lazy, Suspense, useState } from 'react'
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
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
import { useTranslation } from 'react-i18next'
import {
  useViewSettings,
  type BgRanges,
  type ChartHours,
  type TirHours,
} from '../hooks/useViewSettings'
import { useStatus, useVerifyAuth } from '../hooks/useNightscoutData'

const AdminDialog = lazy(() =>
  import('./AdminDialog').then((m) => ({ default: m.AdminDialog })),
)

const CHART_OPTIONS: ChartHours[] = [3, 6, 12, 24]
const TIR_OPTIONS: { hours: TirHours; label: string }[] = [
  { hours: 24, label: '24h' },
  { hours: 168, label: '7d' },
  { hours: 336, label: '14d' },
]

export function SettingsMenu() {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const [adminOpen, setAdminOpen] = useState(false)
  const settings = useViewSettings()
  const auth = useVerifyAuth()
  const isAdmin = auth.data?.admin === true
  const status = useStatus()
  const isMmol = status.data?.settings.units === 'mmol/l'
  const { t } = useTranslation()

  return (
    <>
      <Tooltip title={t('settings.open')}>
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
            <Section label={t('settings.sections.graphRange')}>
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

            <Section label={t('settings.sections.tirWindow')}>
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

            <Section label={t('settings.sections.ranges', { unit: isMmol ? 'mmol/L' : 'mg/dL' })}>
              <RangeEditor ranges={settings.ranges} onChange={settings.setRanges} isMmol={isMmol} />
              <Button size="small" onClick={settings.resetRanges} sx={{ alignSelf: 'flex-end' }}>
                {t('settings.ranges.reset')}
              </Button>
            </Section>

            <Divider />

            <Section label={t('settings.sections.chartLine')}>
              <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Typography variant="body2">{t('settings.chart.showLine')}</Typography>
                <Switch
                  size="small"
                  checked={settings.showLine}
                  onChange={(e) => settings.setShowLine(e.target.checked)}
                />
              </Stack>
              <Stack spacing={0.5} sx={{ opacity: settings.showLine ? 1 : 0.4 }}>
                <Typography variant="body2">{t('settings.chart.smoothing')}</Typography>
                <Slider
                  size="small"
                  value={settings.smoothing}
                  onChange={(_, v) => settings.setSmoothing(v as number)}
                  min={0}
                  max={3}
                  step={1}
                  marks={[
                    { value: 0, label: t('settings.chart.raw') },
                    { value: 1, label: t('settings.chart.light') },
                    { value: 2, label: t('settings.chart.medium') },
                    { value: 3, label: t('settings.chart.heavy') },
                  ]}
                  disabled={!settings.showLine}
                />
              </Stack>
            </Section>

            <Divider />

            <Section label={t('settings.sections.agp')}>
              <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Typography variant="body2">{t('settings.agp.show')}</Typography>
                <Switch
                  size="small"
                  checked={settings.showAgp}
                  onChange={(e) => settings.setShowAgp(e.target.checked)}
                />
              </Stack>
              <Stack sx={{ opacity: settings.showAgp ? 1 : 0.4 }}>
                <ButtonGroup size="small" variant="outlined" fullWidth>
                  {[7, 14, 30].map((d) => (
                    <Button
                      key={d}
                      variant={d === settings.agpDays ? 'contained' : 'outlined'}
                      onClick={() => settings.setAgpDays(d)}
                      disabled={!settings.showAgp}
                    >
                      {t('settings.agp.days', { count: d })}
                    </Button>
                  ))}
                </ButtonGroup>
              </Stack>
            </Section>

            {isAdmin && (
              <>
                <Divider />
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<AdminPanelSettingsIcon />}
                  onClick={() => {
                    setAnchor(null)
                    setAdminOpen(true)
                  }}
                >
                  {t('settings.adminButton')}
                </Button>
              </>
            )}
          </Stack>
        </Box>
      </Popover>
      {adminOpen && (
        <Suspense fallback={null}>
          <AdminDialog open={adminOpen} onClose={() => setAdminOpen(false)} />
        </Suspense>
      )}
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
  isMmol,
}: {
  ranges: BgRanges
  onChange: (next: BgRanges) => void
  isMmol: boolean
}) {
  const { t } = useTranslation()
  // Storage is always mg/dL; the input box shows whichever unit the user picked.
  const update = (key: keyof BgRanges) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = parseFloat(e.target.value)
    if (Number.isNaN(raw)) return
    const mgdl = isMmol ? Math.round(raw * 18) : Math.round(raw)
    onChange({ ...ranges, [key]: mgdl })
  }

  return (
    <Stack spacing={1}>
      <RangeRow
        label={t('settings.ranges.urgentHigh')}
        value={ranges.urgentHigh}
        onChange={update('urgentHigh')}
        color="#e53935"
        isMmol={isMmol}
      />
      <RangeRow
        label={t('settings.ranges.targetHigh')}
        value={ranges.targetHigh}
        onChange={update('targetHigh')}
        color="#ffb300"
        isMmol={isMmol}
      />
      <RangeRow
        label={t('settings.ranges.targetLow')}
        value={ranges.targetLow}
        onChange={update('targetLow')}
        color="#ffb300"
        isMmol={isMmol}
      />
      <RangeRow
        label={t('settings.ranges.urgentLow')}
        value={ranges.urgentLow}
        onChange={update('urgentLow')}
        color="#e53935"
        isMmol={isMmol}
      />
    </Stack>
  )
}

function RangeRow({
  label,
  value,
  onChange,
  color,
  isMmol,
}: {
  label: string
  value: number
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  color: string
  isMmol: boolean
}) {
  // value is always stored mg/dL; show whichever unit is active.
  const displayValue = isMmol ? (Math.round((value / 18) * 10) / 10).toString() : value.toString()
  const unitLabel = isMmol ? 'mmol/L' : 'mg/dL'
  return (
    <Stack direction="row" alignItems="center" spacing={1.5}>
      <Box sx={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: color }} />
      <Typography variant="body2" sx={{ flex: 1 }}>
        {label}
      </Typography>
      <TextField
        size="small"
        type="number"
        value={displayValue}
        onChange={onChange}
        sx={{ width: 150 }}
        slotProps={{
          input: {
            endAdornment: (
              <InputAdornment position="end" sx={{ '& p': { fontSize: 11 } }}>
                {unitLabel}
              </InputAdornment>
            ),
          },
          htmlInput: {
            min: isMmol ? 1.7 : 30,
            max: isMmol ? 33 : 600,
            step: isMmol ? 0.1 : 1,
            style: { textAlign: 'right', paddingRight: 4 },
          },
        }}
      />
    </Stack>
  )
}
