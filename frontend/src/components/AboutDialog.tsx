import {
  Alert,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  Link,
  Stack,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type ActuatorInfo } from '../api/client'

interface Props {
  open: boolean
  onClose: () => void
}

function commitId(info: ActuatorInfo | undefined): { short: string; full: string } | null {
  const id = info?.git?.commit?.id
  if (!id) return null
  if (typeof id === 'string') return { short: id.slice(0, 7), full: id }
  return { short: id.abbrev ?? (id.full ?? '').slice(0, 7), full: id.full ?? id.abbrev ?? '' }
}

function formatTime(iso: string | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString()
}

const REPO_URL = 'https://github.com/oh-deere/nightscout-java'

export function AboutDialog({ open, onClose }: Props) {
  const { t } = useTranslation()
  const info = useQuery({
    queryKey: ['actuator-info'],
    queryFn: api.info,
    enabled: open,
    staleTime: 60_000,
  })

  const commit = commitId(info.data)
  const branch = info.data?.git?.branch
  const dirty = info.data?.git?.dirty
  const commitTime = formatTime(info.data?.git?.commit?.time)
  const buildTime = formatTime(info.data?.build?.time)
  const version = info.data?.build?.version

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('about.title')}</DialogTitle>
      <DialogContent>
        {info.isPending && (
          <Stack alignItems="center" sx={{ py: 3 }}>
            <CircularProgress size={28} />
          </Stack>
        )}
        {info.error && <Alert severity="error">{t('about.loadFailed')}</Alert>}
        {info.data && (
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            {version && <Row label={t('about.version')} value={version} />}
            {commit && (
              <Row
                label={t('about.commit')}
                value={
                  <Link
                    href={`${REPO_URL}/commit/${commit.full}`}
                    target="_blank"
                    rel="noreferrer"
                    underline="hover"
                    sx={{ fontFamily: 'monospace' }}
                  >
                    {commit.short}
                    {dirty ? ' (dirty)' : ''}
                  </Link>
                }
              />
            )}
            {branch && <Row label={t('about.branch')} value={branch} />}
            <Row label={t('about.commitTime')} value={commitTime} />
            <Row label={t('about.buildTime')} value={buildTime} />
          </Stack>
        )}
      </DialogContent>
    </Dialog>
  )
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Stack direction="row" justifyContent="space-between" alignItems="baseline" spacing={2}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
        {value}
      </Typography>
    </Stack>
  )
}
