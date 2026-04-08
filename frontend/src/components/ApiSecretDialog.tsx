import { useState } from 'react'
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField,
} from '@mui/material'
import { useTranslation } from 'react-i18next'
import { setApiSecretHash } from '../api/client'
import { sha1Hex } from '../utils/sha1'

interface Props {
  open: boolean
  onClose: () => void
}

export function ApiSecretDialog({ open, onClose }: Props) {
  const [secret, setSecret] = useState('')
  const [busy, setBusy] = useState(false)
  const { t } = useTranslation()

  const handleSave = async () => {
    setBusy(true)
    try {
      const hash = await sha1Hex(secret)
      setApiSecretHash(hash)
      onClose()
      window.location.reload()
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('auth.title')}</DialogTitle>
      <DialogContent>
        <DialogContentText>{t('auth.description')}</DialogContentText>
        <TextField
          autoFocus
          margin="dense"
          type="password"
          fullWidth
          variant="outlined"
          value={secret}
          onChange={(e) => setSecret(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && secret) handleSave()
          }}
          placeholder={t('auth.placeholder')}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleSave} disabled={!secret || busy} variant="contained">
          {t('auth.save')}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
