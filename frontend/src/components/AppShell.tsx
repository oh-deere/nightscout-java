import { useEffect, useState } from 'react'
import { AppBar, Box, IconButton, Toolbar, Tooltip, Typography } from '@mui/material'
import LogoutIcon from '@mui/icons-material/Logout'
import NotificationsIcon from '@mui/icons-material/Notifications'
import NotificationsOffIcon from '@mui/icons-material/NotificationsOff'
import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import i18n from '../i18n'
import { Dashboard } from './Dashboard'
import { ApiSecretDialog } from './ApiSecretDialog'
import { useStatus } from '../hooks/useNightscoutData'
import { ApiError } from '../api/client'
import { clearApiSecretHash, getApiSecretHash } from '../api/client'
import { useNotifications } from '../hooks/useNotifications'
import { SettingsMenu } from './SettingsMenu'

const SUPPORTED_LANGUAGES = ['en', 'sv'] as const

export function AppShell() {
  const status = useStatus()
  const [authOpen, setAuthOpen] = useState(false)
  const notify = useNotifications()
  const queryClient = useQueryClient()
  const { t } = useTranslation()

  // Drive the active language from the runtime setting fetched in /api/v1/status.
  useEffect(() => {
    const lang = status.data?.settings.language
    if (lang && SUPPORTED_LANGUAGES.includes(lang as (typeof SUPPORTED_LANGUAGES)[number])) {
      void i18n.changeLanguage(lang)
    }
  }, [status.data?.settings.language])

  useEffect(() => {
    const hasHash = getApiSecretHash() != null
    const failed = status.error instanceof ApiError && status.error.status === 401
    if (!hasHash || failed) {
      setAuthOpen(true)
    }
  }, [status.error])

  const title = status.data?.settings.customTitle ?? t('app.title')

  const handleNotifyClick = () => {
    if (notify.permission === 'granted') {
      notify.setEnabled(!notify.enabled)
    } else {
      void notify.requestPermission()
    }
  }

  return (
    <Box sx={{ width: '100vw', minHeight: '100vh', overflowX: 'hidden' }}>
      <AppBar position="static" color="transparent" elevation={0}>
        <Toolbar variant="dense">
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 600 }}>
            {title}
          </Typography>
          {notify.supported && (
            <Tooltip title={notify.enabled ? t('app.notificationsOn') : t('app.notificationsEnable')}>
              <IconButton color="inherit" size="small" onClick={handleNotifyClick}>
                {notify.enabled ? <NotificationsIcon /> : <NotificationsOffIcon />}
              </IconButton>
            </Tooltip>
          )}
          <SettingsMenu />
          <Tooltip title={t('app.signOut')}>
            <IconButton
              color="inherit"
              size="small"
              onClick={() => {
                clearApiSecretHash()
                queryClient.removeQueries({ queryKey: ['verifyauth'] })
                setAuthOpen(true)
              }}
              aria-label="sign out"
            >
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Box sx={{ width: '100%' }}>
        {status.data ? <Dashboard notificationsEnabled={notify.enabled} /> : null}
      </Box>
      <ApiSecretDialog open={authOpen} onClose={() => setAuthOpen(false)} />
    </Box>
  )
}
