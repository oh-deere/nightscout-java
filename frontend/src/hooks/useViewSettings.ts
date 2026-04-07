import { createContext, useContext, useState, type ReactNode, createElement } from 'react'

export type ChartHours = 3 | 6 | 12 | 24
export type TirHours = 24 | 168 | 336 // 1d / 7d / 14d

/** All BG threshold/range values are stored in mg/dL internally. */
export interface BgRanges {
  urgentHigh: number
  targetHigh: number
  targetLow: number
  urgentLow: number
}

const DEFAULT_RANGES: BgRanges = {
  urgentHigh: 260,
  targetHigh: 180,
  targetLow: 70,
  urgentLow: 55,
}

const STORAGE_KEY = 'nightscout.viewSettings'

interface ViewSettings {
  chartHours: ChartHours
  tirHours: TirHours
  ranges: BgRanges
  showLine: boolean
  smoothing: number // 0..3 — 0=none, 3=very smooth
  setChartHours: (h: ChartHours) => void
  setTirHours: (h: TirHours) => void
  setRanges: (r: BgRanges) => void
  setShowLine: (b: boolean) => void
  setSmoothing: (n: number) => void
  resetRanges: () => void
}

const ViewSettingsContext = createContext<ViewSettings | null>(null)

interface Stored {
  chartHours: ChartHours
  tirHours: TirHours
  ranges: BgRanges
  showLine: boolean
  smoothing: number
}

function load(): Stored {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<Stored>
      return {
        chartHours: (parsed.chartHours ?? 6) as ChartHours,
        tirHours: (parsed.tirHours ?? 24) as TirHours,
        ranges: { ...DEFAULT_RANGES, ...(parsed.ranges ?? {}) },
        showLine: parsed.showLine ?? true,
        smoothing: parsed.smoothing ?? 1,
      }
    }
  } catch {
    // ignore
  }
  return {
    chartHours: 6,
    tirHours: 24,
    ranges: DEFAULT_RANGES,
    showLine: true,
    smoothing: 1,
  }
}

function save(s: Stored): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(s))
  } catch {
    // ignore
  }
}

export function ViewSettingsProvider({ children }: { children: ReactNode }) {
  const initial = load()
  const [chartHours, setChartHoursState] = useState<ChartHours>(initial.chartHours)
  const [tirHours, setTirHoursState] = useState<TirHours>(initial.tirHours)
  const [ranges, setRangesState] = useState<BgRanges>(initial.ranges)
  const [showLine, setShowLineState] = useState<boolean>(initial.showLine)
  const [smoothing, setSmoothingState] = useState<number>(initial.smoothing)

  const persist = (next: Partial<Stored>) => {
    save({
      chartHours,
      tirHours,
      ranges,
      showLine,
      smoothing,
      ...next,
    })
  }

  const setChartHours = (h: ChartHours) => {
    setChartHoursState(h)
    persist({ chartHours: h })
  }
  const setTirHours = (h: TirHours) => {
    setTirHoursState(h)
    persist({ tirHours: h })
  }
  const setRanges = (r: BgRanges) => {
    setRangesState(r)
    persist({ ranges: r })
  }
  const setShowLine = (b: boolean) => {
    setShowLineState(b)
    persist({ showLine: b })
  }
  const setSmoothing = (n: number) => {
    setSmoothingState(n)
    persist({ smoothing: n })
  }
  const resetRanges = () => setRanges(DEFAULT_RANGES)

  return createElement(
    ViewSettingsContext.Provider,
    {
      value: {
        chartHours,
        tirHours,
        ranges,
        showLine,
        smoothing,
        setChartHours,
        setTirHours,
        setRanges,
        setShowLine,
        setSmoothing,
        resetRanges,
      },
    },
    children,
  )
}

export function useViewSettings(): ViewSettings {
  const ctx = useContext(ViewSettingsContext)
  if (!ctx) {
    throw new Error('useViewSettings must be used within a ViewSettingsProvider')
  }
  return ctx
}
