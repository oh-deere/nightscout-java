import type { Entry, Thresholds } from '../types/nightscout'

export interface TirBuckets {
  total: number
  urgentLow: number
  low: number
  inRange: number
  high: number
  urgentHigh: number
}

export interface TirPercents {
  urgentLow: number
  low: number
  inRange: number
  high: number
  urgentHigh: number
}

export function bucketEntries(entries: Entry[], thresholds: Thresholds): TirBuckets {
  const buckets: TirBuckets = {
    total: 0,
    urgentLow: 0,
    low: 0,
    inRange: 0,
    high: 0,
    urgentHigh: 0,
  }
  for (const e of entries) {
    if (e.type !== 'sgv' || e.sgv == null) continue
    buckets.total++
    if (e.sgv <= thresholds.bgLow) buckets.urgentLow++
    else if (e.sgv < thresholds.bgTargetBottom) buckets.low++
    else if (e.sgv > thresholds.bgHigh) buckets.urgentHigh++
    else if (e.sgv > thresholds.bgTargetTop) buckets.high++
    else buckets.inRange++
  }
  return buckets
}

export function toPercents(b: TirBuckets): TirPercents {
  if (b.total === 0) {
    return { urgentLow: 0, low: 0, inRange: 0, high: 0, urgentHigh: 0 }
  }
  return {
    urgentLow: (b.urgentLow / b.total) * 100,
    low: (b.low / b.total) * 100,
    inRange: (b.inRange / b.total) * 100,
    high: (b.high / b.total) * 100,
    urgentHigh: (b.urgentHigh / b.total) * 100,
  }
}

/**
 * Estimated A1C using the Glucose Management Indicator (GMI) formula. Reference:
 * https://diabetesjournals.org/care/article/41/11/2275/36593
 */
export function estimatedA1c(meanMgdl: number): number {
  return 3.31 + 0.02392 * meanMgdl
}

export function meanGlucose(entries: Entry[]): number {
  const sgvs = entries.filter((e) => e.type === 'sgv' && e.sgv != null).map((e) => e.sgv!)
  if (sgvs.length === 0) return 0
  return sgvs.reduce((sum, v) => sum + v, 0) / sgvs.length
}

export function standardDeviation(entries: Entry[]): number {
  const sgvs = entries.filter((e) => e.type === 'sgv' && e.sgv != null).map((e) => e.sgv!)
  if (sgvs.length === 0) return 0
  const mean = sgvs.reduce((sum, v) => sum + v, 0) / sgvs.length
  const variance = sgvs.reduce((sum, v) => sum + (v - mean) ** 2, 0) / sgvs.length
  return Math.sqrt(variance)
}
