import { useState } from 'react'
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
} from '../api/client'

type TabValue = 'keys' | 'settings' | 'audit'

export function AdminDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [tab, setTab] = useState<TabValue>('keys')

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', pb: 0 }}>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          User & runtime admin
        </Typography>
        <IconButton size="small" onClick={onClose} aria-label="close">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <Tabs value={tab} onChange={(_, v) => setTab(v as TabValue)} sx={{ px: 3 }}>
        <Tab value="keys" label="API keys" />
        <Tab value="settings" label="Runtime settings" />
        <Tab value="audit" label="Audit log" />
      </Tabs>
      <DialogContent dividers sx={{ minHeight: 360 }}>
        {tab === 'keys' && <KeysTab />}
        {tab === 'settings' && <SettingsTab />}
        {tab === 'audit' && <AuditTab />}
      </DialogContent>
    </Dialog>
  )
}

/* ---------- API keys ---------- */

function KeysTab() {
  const qc = useQueryClient()
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
      setError('Name is required')
      return
    }
    createMutation.mutate()
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1.5} alignItems="center">
        <TextField
          size="small"
          label="Key name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          sx={{ flex: 1 }}
        />
        <TextField
          size="small"
          select
          label="Scope"
          value={scope}
          onChange={(e) => setScope(e.target.value as AdminApiKey['scope'])}
          sx={{ width: 140 }}
        >
          <MenuItem value="read">Read</MenuItem>
          <MenuItem value="write">Write</MenuItem>
          <MenuItem value="admin">Admin</MenuItem>
        </TextField>
        <Button variant="contained" onClick={submit} disabled={createMutation.isPending}>
          Create
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
              aria-label="copy token"
            >
              <ContentCopyIcon fontSize="small" />
            </IconButton>
          }
        >
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            Save this token now — it will not be shown again:
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
                <TableCell>Name</TableCell>
                <TableCell>Scope</TableCell>
                <TableCell>Created</TableCell>
                <TableCell>Last used</TableCell>
                <TableCell>Status</TableCell>
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
                      label={k.enabled ? 'Enabled' : 'Revoked'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    {k.enabled && (
                      <Tooltip title="Revoke">
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
                      No API keys yet
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

const KNOWN_KEYS: { key: string; label: string; help: string }[] = [
  { key: 'units', label: 'Units', help: '"mg/dl" or "mmol/l"' },
  { key: 'customTitle', label: 'Custom title', help: 'Header title in the dashboard' },
  { key: 'thresholds.bgHigh', label: 'BG urgent high (mg/dL)', help: 'Number, e.g. 260' },
  { key: 'thresholds.bgTargetTop', label: 'BG target top (mg/dL)', help: 'Number, e.g. 180' },
  { key: 'thresholds.bgTargetBottom', label: 'BG target bottom (mg/dL)', help: 'Number, e.g. 80' },
  { key: 'thresholds.bgLow', label: 'BG urgent low (mg/dL)', help: 'Number, e.g. 55' },
  { key: 'alarmTimeagoWarnMins', label: 'Stale data warn (min)', help: 'Number, e.g. 15' },
  { key: 'alarmTimeagoUrgentMins', label: 'Stale data urgent (min)', help: 'Number, e.g. 30' },
  { key: 'delta.warn', label: 'Rate warn (mg/dL per 5 min)', help: 'Number, e.g. 15' },
  { key: 'delta.urgent', label: 'Rate urgent (mg/dL per 5 min)', help: 'Number, e.g. 25' },
]

function SettingsTab() {
  const qc = useQueryClient()
  const settings = useQuery({ queryKey: ['admin', 'settings'], queryFn: api.admin.listSettings })
  const [key, setKey] = useState('')
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)

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
          label="Key"
          value={key}
          onChange={(e) => setKey(e.target.value)}
          sx={{ flex: 1 }}
          helperText={KNOWN_KEYS.find((k) => k.key === key)?.help ?? 'Pick or type a key'}
          slotProps={{ select: { native: false } }}
        >
          {KNOWN_KEYS.map((k) => (
            <MenuItem key={k.key} value={k.key}>
              {k.label}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          size="small"
          label='Value (JSON or string)'
          value={value}
          onChange={(e) => setValue(e.target.value)}
          sx={{ flex: 2 }}
        />
        <Button
          variant="contained"
          onClick={() => {
            setError(null)
            if (!key.trim()) {
              setError('Key is required')
              return
            }
            putMutation.mutate()
          }}
        >
          Save
        </Button>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}

      {settings.isLoading && <CircularProgress size={24} />}
      {settings.data && (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Key</TableCell>
                <TableCell>Value</TableCell>
                <TableCell>Updated</TableCell>
                <TableCell>By</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {settings.data.map((s: AdminSetting) => (
                <TableRow key={s.key}>
                  <TableCell>{s.key}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', maxWidth: 280, wordBreak: 'break-all' }}>
                    {s.value}
                  </TableCell>
                  <TableCell>{formatDate(s.updatedAt)}</TableCell>
                  <TableCell>{s.updatedBy}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Delete">
                      <IconButton
                        size="small"
                        onClick={() => deleteMutation.mutate(s.key)}
                        aria-label="delete setting"
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
                      No runtime settings yet
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

/* ---------- Audit log ---------- */

function AuditTab() {
  const audit = useQuery({ queryKey: ['admin', 'audit'], queryFn: () => api.admin.audit(100) })

  return (
    <Box>
      {audit.isLoading && <CircularProgress size={24} />}
      {audit.error && <Alert severity="error">Failed to load audit log</Alert>}
      {audit.data && (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>When</TableCell>
                <TableCell>Actor</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Target</TableCell>
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
