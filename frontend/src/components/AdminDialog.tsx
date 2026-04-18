import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import DeleteIcon from '@mui/icons-material/Delete'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  api,
  type AdminApiKey,
  type AdminApiKeyCreated,
  type AdminAuditEntry,
  type AdminSetting,
  type AlarmHistoryEntry,
} from '../api/client'

type TabValue = 'keys' | 'settings' | 'alarms' | 'audit'

export function AdminDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [tab, setTab] = useState<TabValue>('keys')
  const { t } = useTranslation()

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', pb: 0 }}>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          {t('admin.title')}
        </Typography>
        <IconButton size="small" onClick={onClose} aria-label={t('admin.close')}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <Tabs value={tab} onChange={(_, v) => setTab(v as TabValue)} sx={{ px: 3 }}>
        <Tab value="keys" label={t('admin.tabs.keys')} />
        <Tab value="settings" label={t('admin.tabs.settings')} />
        <Tab value="alarms" label={t('admin.tabs.alarms')} />
        <Tab value="audit" label={t('admin.tabs.audit')} />
      </Tabs>
      <DialogContent dividers sx={{ minHeight: 360 }}>
        {tab === 'keys' && <KeysTab />}
        {tab === 'settings' && <SettingsTab />}
        {tab === 'alarms' && <AlarmHistoryTab />}
        {tab === 'audit' && <AuditTab />}
      </DialogContent>
    </Dialog>
  )
}

/* ---------- API keys ---------- */

function KeysTab() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const keys = useQuery({ queryKey: ['admin', 'keys'], queryFn: api.admin.listKeys })
  const [name, setName] = useState('')
  const [scope, setScope] = useState<AdminApiKey['scope']>('write')
  const [created, setCreated] = useState<AdminApiKeyCreated | null>(null)
  const [error, setError] = useState<string | null>(null)

  const createMutation = useMutation({
    mutationFn: () => api.admin.createKey(name, scope),
    onSuccess: (data) => {
      setCreated(data)
      setName('')
      void qc.invalidateQueries({ queryKey: ['admin', 'keys'] })
    },
    onError: (err: Error) => setError(err.message),
  })

  const revokeMutation = useMutation({
    mutationFn: (id: string) => api.admin.revokeKey(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'keys'] }),
  })

  const submit = () => {
    setError(null)
    if (!name.trim()) {
      setError(t('admin.keys.nameRequired'))
      return
    }
    createMutation.mutate()
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1.5} alignItems="center">
        <TextField
          size="small"
          label={t('admin.keys.keyName')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          sx={{ flex: 1 }}
        />
        <TextField
          size="small"
          select
          label={t('admin.keys.scope')}
          value={scope}
          onChange={(e) => setScope(e.target.value as AdminApiKey['scope'])}
          sx={{ width: 140 }}
        >
          <MenuItem value="read">{t('admin.keys.scopeRead')}</MenuItem>
          <MenuItem value="write">{t('admin.keys.scopeWrite')}</MenuItem>
          <MenuItem value="admin">{t('admin.keys.scopeAdmin')}</MenuItem>
        </TextField>
        <Button variant="contained" onClick={submit} disabled={createMutation.isPending}>
          {t('admin.keys.create')}
        </Button>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}

      {created && (
        <Alert
          severity="success"
          onClose={() => setCreated(null)}
          action={
            <IconButton
              size="small"
              onClick={() => navigator.clipboard.writeText(created.token)}
              aria-label={t('admin.keys.copyToken')}
            >
              <ContentCopyIcon fontSize="small" />
            </IconButton>
          }
        >
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {t('admin.keys.saveTokenWarning')}
          </Typography>
          <Typography
            variant="body2"
            sx={{ fontFamily: 'monospace', wordBreak: 'break-all', mt: 0.5 }}
          >
            {created.token}
          </Typography>
        </Alert>
      )}

      {keys.isLoading && <CircularProgress size={24} />}
      {keys.error && <Alert severity="error">Failed to load keys</Alert>}
      {keys.data && (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('admin.keys.table.name')}</TableCell>
                <TableCell>{t('admin.keys.table.scope')}</TableCell>
                <TableCell>{t('admin.keys.table.created')}</TableCell>
                <TableCell>{t('admin.keys.table.lastUsed')}</TableCell>
                <TableCell>{t('admin.keys.table.status')}</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {keys.data.map((k) => (
                <TableRow key={k.id}>
                  <TableCell>{k.name}</TableCell>
                  <TableCell>
                    <Chip size="small" label={k.scope} />
                  </TableCell>
                  <TableCell>{formatDate(k.createdAt)}</TableCell>
                  <TableCell>{k.lastUsedAt ? formatDate(k.lastUsedAt) : '—'}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={k.enabled ? 'success' : 'default'}
                      label={k.enabled ? t('admin.keys.enabled') : t('admin.keys.revoked')}
                    />
                  </TableCell>
                  <TableCell align="right">
                    {k.enabled && (
                      <Tooltip title={t('admin.keys.revoke')}>
                        <IconButton
                          size="small"
                          onClick={() => revokeMutation.mutate(k.id)}
                          aria-label="revoke"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {keys.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>
                    <Typography variant="body2" color="text.secondary" textAlign="center">
                      {t('admin.keys.table.empty')}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Stack>
  )
}

/* ---------- Runtime settings ---------- */

// Backend setting key → i18n bundle slug. Extending this list grows the
// dropdown in SettingsTab; the resolved label/help comes from
// admin.knownKeys.<slug> in the active language bundle.
const KNOWN_KEY_DEFINITIONS: { key: string; slug: string }[] = [
  { key: 'units', slug: 'units' },
  { key: 'customTitle', slug: 'customTitle' },
  { key: 'theme', slug: 'theme' },
  { key: 'language', slug: 'language' },
  { key: 'timeFormat', slug: 'timeFormat' },
  { key: 'nightMode', slug: 'nightMode' },
  { key: 'alarmTypes', slug: 'alarmTypes' },
  { key: 'authDefaultRoles', slug: 'authDefaultRoles' },
  { key: 'devicestatusAdvanced', slug: 'devicestatusAdvanced' },
  { key: 'bolusRenderOver', slug: 'bolusRenderOver' },
  { key: 'thresholds.bgHigh', slug: 'bgUrgentHigh' },
  { key: 'thresholds.bgTargetTop', slug: 'bgTargetTop' },
  { key: 'thresholds.bgTargetBottom', slug: 'bgTargetBottom' },
  { key: 'thresholds.bgLow', slug: 'bgUrgentLow' },
  { key: 'alarmTimeagoWarnMins', slug: 'timeagoWarn' },
  { key: 'alarmTimeagoUrgentMins', slug: 'timeagoUrgent' },
  { key: 'delta.warn', slug: 'deltaWarn' },
  { key: 'delta.urgent', slug: 'deltaUrgent' },
  { key: 'sage.info', slug: 'sageInfo' },
  { key: 'sage.warn', slug: 'sageWarn' },
  { key: 'sage.urgent', slug: 'sageUrgent' },
]

function SettingsTab() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const settings = useQuery({ queryKey: ['admin', 'settings'], queryFn: api.admin.listSettings })
  const [key, setKey] = useState('')
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)

  // Resolve labels + help text against the active language each render so
  // language switches at runtime are picked up immediately.
  const knownKeys = useMemo(
    () =>
      KNOWN_KEY_DEFINITIONS.map((d) => ({
        key: d.key,
        label: t(`admin.knownKeys.${d.slug}.label`),
        help: t(`admin.knownKeys.${d.slug}.help`),
      })),
    [t],
  )

  const putMutation = useMutation({
    mutationFn: () => {
      let parsed: unknown
      try {
        parsed = JSON.parse(value)
      } catch {
        parsed = value
      }
      return api.admin.putSetting(key, parsed)
    },
    onSuccess: () => {
      setKey('')
      setValue('')
      void qc.invalidateQueries({ queryKey: ['admin', 'settings'] })
    },
    onError: (err: Error) => setError(err.message),
  })

  const deleteMutation = useMutation({
    mutationFn: (k: string) => api.admin.deleteSetting(k),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'settings'] }),
  })

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1.5}>
        <TextField
          size="small"
          select
          label={t('admin.settings.keyLabel')}
          value={key}
          onChange={(e) => setKey(e.target.value)}
          sx={{ flex: 1 }}
          helperText={knownKeys.find((k) => k.key === key)?.help ?? t('admin.settings.keyHelper')}
          slotProps={{ select: { native: false } }}
        >
          {knownKeys.map((k) => (
            <MenuItem key={k.key} value={k.key}>
              {k.label}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          size="small"
          label={t('admin.settings.valueLabel')}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          sx={{ flex: 2 }}
        />
        <Button
          variant="contained"
          onClick={() => {
            setError(null)
            if (!key.trim()) {
              setError(t('admin.settings.keyRequired'))
              return
            }
            putMutation.mutate()
          }}
        >
          {t('admin.settings.save')}
        </Button>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}

      {settings.isLoading && <CircularProgress size={24} />}
      {settings.data && (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('admin.settings.table.key')}</TableCell>
                <TableCell>{t('admin.settings.table.value')}</TableCell>
                <TableCell>{t('admin.settings.table.updated')}</TableCell>
                <TableCell>{t('admin.settings.table.by')}</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {settings.data.map((s: AdminSetting) => (
                <TableRow key={s.key}>
                  <TableCell>{s.key}</TableCell>
                  <TableCell
                    sx={{ fontFamily: 'monospace', maxWidth: 280, wordBreak: 'break-all' }}
                  >
                    {s.value}
                  </TableCell>
                  <TableCell>{formatDate(s.updatedAt)}</TableCell>
                  <TableCell>{s.updatedBy}</TableCell>
                  <TableCell align="right">
                    <Tooltip title={t('admin.settings.delete')}>
                      <IconButton
                        size="small"
                        onClick={() => deleteMutation.mutate(s.key)}
                        aria-label={t('admin.settings.delete')}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
              {settings.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <Typography variant="body2" color="text.secondary" textAlign="center">
                      {t('admin.settings.table.empty')}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Stack>
  )
}

/* ---------- Alarm history ---------- */

function AlarmHistoryTab() {
  const { t } = useTranslation()
  const history = useQuery({ queryKey: ['admin', 'alarms'], queryFn: () => api.alarmHistory(100) })

  return (
    <Box>
      {history.isLoading && <CircularProgress size={24} />}
      {history.error && <Alert severity="error">{t('admin.alarms.loadFailed')}</Alert>}
      {history.data && (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('admin.alarms.table.when')}</TableCell>
                <TableCell>{t('admin.alarms.table.type')}</TableCell>
                <TableCell>{t('admin.alarms.table.level')}</TableCell>
                <TableCell>{t('admin.alarms.table.title')}</TableCell>
                <TableCell>{t('admin.alarms.table.message')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {history.data.map((e: AlarmHistoryEntry) => (
                <TableRow key={e.id}>
                  <TableCell>{formatDate(e.occurredAt)}</TableCell>
                  <TableCell>
                    <Chip size="small" label={e.type} />
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={e.level >= 3 ? 'error' : 'warning'}
                      label={
                        e.level >= 3 ? t('admin.alarms.level.urgent') : t('admin.alarms.level.warn')
                      }
                    />
                  </TableCell>
                  <TableCell>{e.title}</TableCell>
                  <TableCell sx={{ color: 'text.secondary' }}>{e.message ?? ''}</TableCell>
                </TableRow>
              ))}
              {history.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <Typography variant="body2" color="text.secondary" textAlign="center">
                      {t('admin.alarms.table.empty')}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}

/* ---------- Audit log ---------- */

function AuditTab() {
  const { t } = useTranslation()
  const audit = useQuery({ queryKey: ['admin', 'audit'], queryFn: () => api.admin.audit(100) })

  return (
    <Box>
      {audit.isLoading && <CircularProgress size={24} />}
      {audit.error && <Alert severity="error">{t('admin.audit.loadFailed')}</Alert>}
      {audit.data && (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('admin.audit.table.when')}</TableCell>
                <TableCell>{t('admin.audit.table.actor')}</TableCell>
                <TableCell>{t('admin.audit.table.action')}</TableCell>
                <TableCell>{t('admin.audit.table.target')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {audit.data.map((e: AdminAuditEntry) => (
                <TableRow key={e.id}>
                  <TableCell>{formatDate(e.occurredAt)}</TableCell>
                  <TableCell>
                    <Stack>
                      <Typography variant="body2">{e.actorSubject}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {e.actorKind}
                      </Typography>
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={e.action} />
                  </TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{e.target}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}
