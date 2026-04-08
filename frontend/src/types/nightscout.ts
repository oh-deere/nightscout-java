export type Direction =
  | 'DoubleUp'
  | 'SingleUp'
  | 'FortyFiveUp'
  | 'Flat'
  | 'FortyFiveDown'
  | 'SingleDown'
  | 'DoubleDown'
  | 'NOT COMPUTABLE'
  | 'RATE OUT OF RANGE'

export interface Entry {
  _id: string
  type: 'sgv' | 'mbg' | 'cal'
  date: number
  dateString?: string
  sysTime?: string
  sgv?: number
  direction?: Direction
  noise?: number
  device?: string
  utcOffset?: number
}

export interface Thresholds {
  bgHigh: number
  bgTargetTop: number
  bgTargetBottom: number
  bgLow: number
}

export interface NightscoutSettings {
  units: 'mg/dl' | 'mmol/l'
  timeFormat: number
  theme: string
  language: string
  customTitle: string
  showPlugins: string
  enable: string[]
  thresholds: Thresholds
  alarmTypes: string[]
  authDefaultRoles: string
  nightMode: boolean
  alarmTimeagoWarnMins?: number
  alarmTimeagoUrgentMins?: number
}

export interface NightscoutStatus {
  status: string
  name: string
  version: string
  serverTime: string
  serverTimeEpoch: number
  apiEnabled: boolean
  careportalEnabled: boolean
  boluscalcEnabled: boolean
  runtimeState: string
  settings: NightscoutSettings
}

export interface Treatment {
  _id: string
  eventType: string
  created_at: string
  enteredBy?: string
  notes?: string
  insulin?: number
  carbs?: number
  glucose?: number
  glucoseType?: string
  duration?: number
  utcOffset?: number
}

export interface PluginResult {
  name: string
  label: string
  value: string
  level?: string
  data?: Record<string, unknown>
}

export interface PluginProperties {
  bgnow?: PluginResult
  iob?: PluginResult
  cob?: PluginResult
  ar2?: PluginResult
  cage?: PluginResult
  sage?: PluginResult
  iage?: PluginResult
  bage?: PluginResult
  alarm?: AlarmInfo
  alarms?: AlarmInfo[]
  snoozes?: Record<string, string>
  pump?: PluginResult
  agpRank?: { percentile: number; bucketMinute: number; p50: number }
}

export interface PumpData {
  source?: 'loop' | 'openaps'
  pumpReservoir?: number
  pumpManufacturer?: string
  pumpModel?: string
  pumpBatteryPercent?: number
  pumpBatteryVoltage?: number
  loopName?: string
  loopVersion?: string
  loopIob?: number
  loopCob?: number
  openapsReason?: string
  openapsEventualBg?: number
  loopTimestamp?: string
  loopAgeMinutes?: number
  loopStale?: boolean
}

export interface AlarmInfo {
  level: number
  title: string
  message: string
  type: string
}
