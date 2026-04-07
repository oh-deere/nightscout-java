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
import { setApiSecretHash } from '../api/client'
import { sha1Hex } from '../utils/sha1'

interface Props {
  open: boolean
  onClose: () => void
}

export function ApiSecretDialog({ open, onClose }: Props) {
  const [secret, setSecret] = useState('')
  const [busy, setBusy] = useState(false)

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
      <DialogTitle>Authentication required</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Enter the Nightscout API secret to view glucose data. The secret is hashed locally
          and stored in your browser only.
        </DialogContentText>
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
          placeholder="API_SECRET"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleSave} disabled={!secret || busy} variant="contained">
          Save
        </Button>
      </DialogActions>
    </Dialog>
  )
}
