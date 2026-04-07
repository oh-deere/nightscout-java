import { useEffect, useState } from 'react'
import { AppBar, Box, IconButton, Toolbar, Tooltip, Typography } from '@mui/material'
import LogoutIcon from '@mui/icons-material/Logout'
import NotificationsIcon from '@mui/icons-material/Notifications'
import NotificationsOffIcon from '@mui/icons-material/NotificationsOff'
import { Dashboard } from './Dashboard'
import { ApiSecretDialog } from './ApiSecretDialog'
import { useStatus } from '../hooks/useNightscoutData'
import { ApiError } from '../api/client'
import { clearApiSecretHash, getApiSecretHash } from '../api/client'
import { useNotifications } from '../hooks/useNotifications'
import { SettingsMenu } from './SettingsMenu'

export function AppShell() {
  const status = useStatus()
  const [authOpen, setAuthOpen] = useState(false)
  const notify = useNotifications()

  useEffect(() => {
    const hasHash = getApiSecretHash() != null
    const failed = status.error instanceof ApiError && status.error.status === 401
    if (!hasHash || failed) {
      setAuthOpen(true)
    }
  }, [status.error])

  const title = status.data?.settings.customTitle ?? 'Nightscout'

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
            <Tooltip title={notify.enabled ? 'Notifications on' : 'Enable notifications'}>
              <IconButton color="inherit" size="small" onClick={handleNotifyClick}>
                {notify.enabled ? <NotificationsIcon /> : <NotificationsOffIcon />}
              </IconButton>
            </Tooltip>
          )}
          <SettingsMenu />
          <Tooltip title="Sign out">
            <IconButton
              color="inherit"
              size="small"
              onClick={() => {
                clearApiSecretHash()
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
