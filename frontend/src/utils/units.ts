/** Convert mg/dL to display units (mg/dl or mmol/l). */
export function toDisplayBg(mgdl: number, units: 'mg/dl' | 'mmol/l'): number {
  if (units === 'mmol/l') {
    return Math.round((mgdl / 18.0) * 10) / 10
  }
  return Math.round(mgdl)
}

export function formatBg(mgdl: number, units: 'mg/dl' | 'mmol/l'): string {
  const v = toDisplayBg(mgdl, units)
  return units === 'mmol/l' ? v.toFixed(1) : String(v)
}

export function formatDelta(mgdl: number, units: 'mg/dl' | 'mmol/l'): string {
  const sign = mgdl > 0 ? '+' : ''
  if (units === 'mmol/l') {
    return sign + (Math.round((mgdl / 18.0) * 10) / 10).toFixed(1)
  }
  return sign + Math.round(mgdl)
}

export const DIRECTION_ARROW: Record<string, string> = {
  DoubleUp: '\u21c8',
  SingleUp: '\u2191',
  FortyFiveUp: '\u2197',
  Flat: '\u2192',
  FortyFiveDown: '\u2198',
  SingleDown: '\u2193',
  DoubleDown: '\u21ca',
  'NOT COMPUTABLE': '-',
  'RATE OUT OF RANGE': '\u21d5',
}
