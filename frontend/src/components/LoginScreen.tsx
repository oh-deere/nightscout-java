import { useState } from 'react'
import { Box, Button, Divider, Paper, Stack, TextField, Typography } from '@mui/material'
import LoginIcon from '@mui/icons-material/Login'
import { useTranslation } from 'react-i18next'
import { setApiSecretHash } from '../api/client'
import { useStatus } from '../hooks/useNightscoutData'
import { sha1Hex } from '../utils/sha1'

export function LoginScreen() {
  const [secret, setSecret] = useState('')
  const [busy, setBusy] = useState(false)
  const { t } = useTranslation()
  const status = useStatus()
  const oauthProviders = status.data?.oauth?.enabled ? (status.data.oauth.providers ?? []) : []
  const title = status.data?.settings.customTitle ?? t('app.title')

  const handleSave = async () => {
    setBusy(true)
    try {
      const hash = await sha1Hex(secret)
      setApiSecretHash(hash)
      window.location.reload()
    } finally {
      setBusy(false)
    }
  }

  return (
    <Box
      sx={{
        width: '100vw',
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Paper elevation={3} sx={{ p: 4, maxWidth: 380, width: '100%' }}>
        <Typography variant="h5" sx={{ mb: 0.5, fontWeight: 600 }}>
          {title}
        </Typography>
        <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 3 }}>
          {t('auth.title')}
        </Typography>
        {oauthProviders.length > 0 && (
          <Stack spacing={1} sx={{ mb: 2 }}>
            {oauthProviders.map((p) => (
              <Button
                key={p.id}
                variant="contained"
                color="primary"
                fullWidth
                startIcon={<LoginIcon />}
                href={p.authorizeUrl}
              >
                {t('auth.signInWith', { provider: p.label })}
              </Button>
            ))}
            <Divider sx={{ my: 1 }}>
              <Typography variant="caption" color="text.secondary">
                {t('auth.orApiSecret')}
              </Typography>
            </Divider>
          </Stack>
        )}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {t('auth.description')}
        </Typography>
        <TextField
          autoFocus
          type="password"
          fullWidth
          variant="outlined"
          size="small"
          value={secret}
          onChange={(e) => setSecret(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && secret) void handleSave()
          }}
          placeholder={t('auth.placeholder')}
          sx={{ mb: 2 }}
        />
        <Button
          onClick={() => void handleSave()}
          disabled={!secret || busy}
          variant="contained"
          fullWidth
        >
          {t('auth.save')}
        </Button>
      </Paper>
    </Box>
  )
}
