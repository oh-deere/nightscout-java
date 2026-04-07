import { useEffect, useState } from 'react'
import {
  AppBar,
  Box,
  Container,
  IconButton,
  Toolbar,
  Typography,
} from '@mui/material'
import LogoutIcon from '@mui/icons-material/Logout'
import { Dashboard } from './Dashboard'
import { ApiSecretDialog } from './ApiSecretDialog'
import { useStatus } from '../hooks/useNightscoutData'
import { ApiError } from '../api/client'
import { clearApiSecretHash, getApiSecretHash } from '../api/client'

export function AppShell() {
  const status = useStatus()
  const [authOpen, setAuthOpen] = useState(false)

  // Trigger auth dialog on 401 from any query, OR if we have no secret stored
  useEffect(() => {
    const hasHash = getApiSecretHash() != null
    const failed = status.error instanceof ApiError && status.error.status === 401
    if (!hasHash || failed) {
      setAuthOpen(true)
    }
  }, [status.error])

  const title = status.data?.settings.customTitle ?? 'Nightscout'

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="static" color="transparent" elevation={0}>
        <Toolbar variant="dense">
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 600 }}>
            {title}
          </Typography>
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
        </Toolbar>
      </AppBar>
      <Container maxWidth="md" sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {status.data ? <Dashboard /> : null}
      </Container>
      <ApiSecretDialog open={authOpen} onClose={() => setAuthOpen(false)} />
    </Box>
  )
}
