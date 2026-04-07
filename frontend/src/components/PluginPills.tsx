import { Chip, Stack } from '@mui/material'
import type { PluginProperties } from '../types/nightscout'

interface Props {
  properties: PluginProperties | undefined
}

export function PluginPills({ properties }: Props) {
  if (!properties) return null

  const pills: Array<{ label: string; value: string; color?: 'default' | 'warning' | 'error' }> =
    []

  const addPlugin = (
    p: { label: string; value: string; level?: string } | undefined,
  ) => {
    if (!p || !p.value || p.value === 'N/A') return
    const color = p.level === 'urgent' ? 'error' : p.level === 'warn' ? 'warning' : 'default'
    pills.push({ label: p.label, value: p.value, color })
  }

  // IOB and COB live in the BgHeader — only show ages here
  addPlugin(properties.cage)
  addPlugin(properties.sage)
  addPlugin(properties.iage)
  addPlugin(properties.bage)

  if (pills.length === 0) return null

  return (
    <Stack direction="row" spacing={1} flexWrap="wrap" justifyContent="center" useFlexGap>
      {pills.map((p) => (
        <Chip
          key={p.label}
          label={`${p.label}: ${p.value}`}
          color={p.color}
          variant="outlined"
          size="small"
        />
      ))}
    </Stack>
  )
}
