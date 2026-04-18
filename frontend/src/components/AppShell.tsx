import { useEffect } from 'react'
import {
  AppBar,
  Box,
  Chip,
  CircularProgress,
  IconButton,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material'
import LogoutIcon from '@mui/icons-material/Logout'
import NotificationsIcon from '@mui/icons-material/Notifications'
import NotificationsOffIcon from '@mui/icons-material/NotificationsOff'
import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import i18n from '../i18n'
import { Dashboard } from './Dashboard'
import { LoginScreen } from './LoginScreen'
import { useStatus, useVerifyAuth } from '../hooks/useNightscoutData'
import { clearApiSecretHash } from '../api/client'
import { useNotifications } from '../hooks/useNotifications'
import { SettingsMenu } from './SettingsMenu'

const SUPPORTED_LANGUAGES = ['en', 'sv'] as const

export function AppShell() {
  const status = useStatus()
  const auth = useVerifyAuth()
  const notify = useNotifications()
  const queryClient = useQueryClient()
  const { t } = useTranslation()

  useEffect(() => {
    const lang = status.data?.settings.language
    if (lang && SUPPORTED_LANGUAGES.includes(lang as (typeof SUPPORTED_LANGUAGES)[number])) {
      void i18n.changeLanguage(lang)
    }
  }, [status.data?.settings.language])

  if (auth.isPending) {
    return (
      <Box
        sx={{
          width: '100vw',
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <CircularProgress />
      </Box>
    )
  }

  if (auth.data?.status !== 200) {
    return <LoginScreen />
  }

  const title = status.data?.settings.customTitle ?? t('app.title')
  const subject = auth.data.sub ?? ''

  const handleNotifyClick = () => {
    if (notify.permission === 'granted') {
      notify.setEnabled(!notify.enabled)
    } else {
      void notify.requestPermission()
    }
  }

  const handleSignOut = () => {
    clearApiSecretHash()
    // Tear down the OAuth session too if there is one. Fire-and-forget;
    // Spring's default /logout clears the session.
    void fetch('/logout', { method: 'POST', credentials: 'same-origin' }).catch(() => undefined)
    queryClient.removeQueries({ queryKey: ['verifyauth'] })
  }

  return (
    <Box sx={{ width: '100vw', minHeight: '100vh', overflowX: 'hidden' }}>
      <AppBar position="static" color="transparent" elevation={0}>
        <Toolbar variant="dense">
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 600 }}>
            {title}
          </Typography>
          {subject && (
            <Tooltip title={t('auth.signedInAs', { subject })}>
              <Chip size="small" label={subject} variant="outlined" sx={{ mr: 1 }} />
            </Tooltip>
          )}
          {notify.supported && (
            <Tooltip
              title={notify.enabled ? t('app.notificationsOn') : t('app.notificationsEnable')}
            >
              <IconButton color="inherit" size="small" onClick={handleNotifyClick}>
                {notify.enabled ? <NotificationsIcon /> : <NotificationsOffIcon />}
              </IconButton>
            </Tooltip>
          )}
          <SettingsMenu />
          <Tooltip title={t('app.signOut')}>
            <IconButton color="inherit" size="small" onClick={handleSignOut} aria-label="sign out">
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Box sx={{ width: '100%' }}>
        {status.data ? <Dashboard notificationsEnabled={notify.enabled} /> : null}
      </Box>
    </Box>
  )
}
