import { Alert, Button, IconButton, Stack, Tooltip, Typography } from '@mui/material'
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive'
import NotificationsPausedIcon from '@mui/icons-material/NotificationsPaused'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { getApiSecretHash } from '../api/client'
import type { AlarmInfo, PluginProperties } from '../types/nightscout'

const SNOOZE_MINUTES = 30

function authHeaders(): HeadersInit {
  const hash = getApiSecretHash()
  return hash ? { 'api-secret': hash } : {}
}

async function snoozeAlarm(group: string): Promise<void> {
  const res = await fetch(
    `/api/v1/notifications/ack?level=2&group=${encodeURIComponent(group)}&silenceMinutes=${SNOOZE_MINUTES}`,
    { method: 'GET', headers: authHeaders() },
  )
  if (!res.ok) {
    throw new Error(`Snooze failed: ${res.status}`)
  }
}

async function clearSnooze(type: string): Promise<void> {
  const res = await fetch(`/api/v1/notifications/snooze/${encodeURIComponent(type)}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  if (!res.ok) {
    throw new Error(`Clear snooze failed: ${res.status}`)
  }
}

interface Props {
  properties?: PluginProperties
}

export function AlarmBanner({ properties }: Props) {
  const queryClient = useQueryClient()
  const { t } = useTranslation()
  const alarms: AlarmInfo[] = properties?.alarms ?? (properties?.alarm ? [properties.alarm] : [])
  const snoozes = properties?.snoozes ?? {}

  const snoozeMutation = useMutation({
    mutationFn: snoozeAlarm,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['properties'] }),
  })

  const clearMutation = useMutation({
    mutationFn: clearSnooze,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['properties'] }),
  })

  if (alarms.length === 0 && Object.keys(snoozes).length === 0) {
    return null
  }

  return (
    <Stack spacing={1}>
      {alarms.map((a) => (
        <Alert
          key={a.type}
          severity={a.level >= 3 ? 'error' : 'warning'}
          action={
            <Button
              size="small"
              color="inherit"
              startIcon={<NotificationsPausedIcon />}
              onClick={() => snoozeMutation.mutate(a.type)}
              disabled={snoozeMutation.isPending}
            >
              {t('alarms.snooze', { minutes: SNOOZE_MINUTES })}
            </Button>
          }
        >
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {a.title}
          </Typography>
          <Typography variant="body2">{a.message}</Typography>
        </Alert>
      ))}
      {Object.entries(snoozes).map(([type, until]) => (
        <Alert
          key={`snooze-${type}`}
          severity="info"
          icon={<NotificationsPausedIcon />}
          action={
            <Tooltip title={t('alarms.clearSnooze')}>
              <IconButton
                size="small"
                color="inherit"
                onClick={() => clearMutation.mutate(type)}
                disabled={clearMutation.isPending}
                aria-label={`unsnooze ${type}`}
              >
                <NotificationsActiveIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          }
        >
          <Typography variant="body2">
            {t('alarms.snoozedUntil', {
              type,
              time: new Date(until).toLocaleTimeString(),
            })}
          </Typography>
        </Alert>
      ))}
    </Stack>
  )
}
