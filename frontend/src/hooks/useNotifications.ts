import { useEffect, useRef, useState } from 'react'
import type { NightscoutSettings, Entry } from '../types/nightscout'
import { formatBg } from '../utils/units'

const NOTIFY_KEY = 'nightscout.notifications.enabled'

type AlarmKey = 'urgentHigh' | 'high' | 'low' | 'urgentLow' | 'stale' | null

function classify(entry: Entry, settings: NightscoutSettings): AlarmKey {
  const ageMins = (Date.now() - entry.date) / 60000
  if (ageMins > settings.thresholds.bgLow && ageMins > 30) return 'stale'
  if (entry.sgv == null) return null
  if (entry.sgv >= settings.thresholds.bgHigh) return 'urgentHigh'
  if (entry.sgv <= settings.thresholds.bgLow) return 'urgentLow'
  if (entry.sgv > settings.thresholds.bgTargetTop) return 'high'
  if (entry.sgv < settings.thresholds.bgTargetBottom) return 'low'
  return null
}

function isStale(entry: Entry, warnMins = 15): boolean {
  return (Date.now() - entry.date) / 60000 > warnMins
}

export function useNotifications() {
  const supported = typeof window !== 'undefined' && 'Notification' in window
  const [permission, setPermission] = useState<NotificationPermission>(
    supported ? Notification.permission : 'default',
  )
  const [enabled, setEnabledState] = useState<boolean>(
    () => supported && localStorage.getItem(NOTIFY_KEY) === 'true',
  )

  const requestPermission = async () => {
    if (!supported) return false
    const result = await Notification.requestPermission()
    setPermission(result)
    if (result === 'granted') {
      setEnabledState(true)
      localStorage.setItem(NOTIFY_KEY, 'true')
      return true
    }
    return false
  }

  const setEnabled = (next: boolean) => {
    setEnabledState(next)
    localStorage.setItem(NOTIFY_KEY, String(next))
  }

  return {
    supported,
    permission,
    enabled: enabled && permission === 'granted',
    requestPermission,
    setEnabled,
  }
}

/**
 * Watches the current entry and fires a browser notification when an alarm condition is
 * raised (only on transition, not on every poll). Stale data is also a transition.
 */
export function useAlarmNotifier(
  current: Entry | null,
  settings: NightscoutSettings | undefined,
  enabled: boolean,
) {
  const lastAlarmRef = useRef<AlarmKey>(null)
  const lastStaleRef = useRef<boolean>(false)

  useEffect(() => {
    if (!enabled || !current || !settings || typeof Notification === 'undefined') {
      return
    }

    const stale = isStale(current, settings.alarmTimeagoWarnMins ?? 15)
    const alarm = classify(current, settings)

    // Stale data alarm transition
    if (stale && !lastStaleRef.current) {
      const ageMins = Math.round((Date.now() - current.date) / 60000)
      new Notification('Stale data', {
        body: `No glucose readings for ${ageMins} minutes`,
        tag: 'stale',
        requireInteraction: false,
      })
    }
    lastStaleRef.current = stale

    if (stale) {
      lastAlarmRef.current = null
      return
    }

    // BG alarm transition
    if (alarm && alarm !== lastAlarmRef.current) {
      const title =
        alarm === 'urgentHigh'
          ? 'Urgent High'
          : alarm === 'urgentLow'
            ? 'Urgent Low'
            : alarm === 'high'
              ? 'High'
              : 'Low'
      const body = current.sgv
        ? `${formatBg(current.sgv, settings.units)} ${settings.units}`
        : ''
      new Notification(title, {
        body,
        tag: alarm,
        requireInteraction: alarm === 'urgentHigh' || alarm === 'urgentLow',
      })
    }
    lastAlarmRef.current = alarm
  }, [current, settings, enabled])
}
