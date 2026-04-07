import { useMemo } from 'react'
import type { NightscoutSettings } from '../types/nightscout'
import { useViewSettings } from './useViewSettings'

/**
 * Merges server-side Nightscout settings with the user's local range overrides so that
 * downstream components (chart, TIR, alarms) all see a single set of effective thresholds.
 */
export function useEffectiveSettings(serverSettings: NightscoutSettings): NightscoutSettings {
  const view = useViewSettings()
  return useMemo(
    () => ({
      ...serverSettings,
      thresholds: {
        bgHigh: view.ranges.urgentHigh,
        bgTargetTop: view.ranges.targetHigh,
        bgTargetBottom: view.ranges.targetLow,
        bgLow: view.ranges.urgentLow,
      },
    }),
    [serverSettings, view.ranges],
  )
}
