import { createTheme } from '@mui/material/styles'

/**
 * Nightscout-style "colors" theme — dark background with high-contrast BG levels.
 */
export const theme = createTheme({
  palette: {
    mode: 'dark',
    background: {
      default: '#0a0a0a',
      paper: '#161616',
    },
    primary: {
      main: '#4caf50',
    },
  },
  typography: {
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
  },
})

/**
 * Color a BG value based on thresholds.
 */
export function bgColor(
  sgvMgdl: number,
  thresholds: { bgHigh: number; bgTargetTop: number; bgTargetBottom: number; bgLow: number },
): string {
  if (sgvMgdl >= thresholds.bgHigh) return '#e53935' // urgent high — red
  if (sgvMgdl >= thresholds.bgTargetTop) return '#ffb300' // high — amber
  if (sgvMgdl <= thresholds.bgLow) return '#e53935' // urgent low — red
  if (sgvMgdl <= thresholds.bgTargetBottom) return '#ffb300' // low — amber
  return '#4caf50' // in range — green
}
