import { useCallback, useMemo } from 'react'
import { useNowMs } from '../hooks/useNowMs'
import { Group } from '@visx/group'
import { scaleLinear, scaleTime } from '@visx/scale'
import { AxisBottom, AxisLeft } from '@visx/axis'
import { GridRows } from '@visx/grid'
import { Area, Circle, Bar, Line, LinePath } from '@visx/shape'
import { ParentSize } from '@visx/responsive'
import { curveLinear, curveMonotoneX, curveCatmullRom, curveBasis } from '@visx/curve'
import { localPoint } from '@visx/event'
import { useTooltip, TooltipWithBounds, defaultStyles } from '@visx/tooltip'
import { bisector } from 'd3-array'
import type { AgpBucket } from '../api/client'
import type { Entry, NightscoutSettings, Treatment } from '../types/nightscout'
import { bgColor } from '../theme/theme'
import { DIRECTION_ARROW, formatBg } from '../utils/units'

interface Props {
  entries: Entry[]
  treatments?: Treatment[]
  settings: NightscoutSettings
  hours: number
  showLine?: boolean
  /** 0=raw, 1=monotone (default), 2=catmull-rom, 3=basis (heavy) */
  smoothing?: number
  /** AGP percentile bands. When present and non-empty the overlay renders behind the trace. */
  agpBuckets?: AgpBucket[]
}

/** One sample of the AGP overlay at a real chart timestamp. */
interface AgpSample {
  date: number
  p5: number
  p25: number
  p50: number
  p75: number
  p95: number
}

/**
 * Project AGP buckets (which are indexed by time-of-day in the local tz) onto the
 * chart's actual timestamp axis. We sample every 5 minutes inside the visible window
 * since the underlying SGV cadence is 5 min — that's smooth enough for the bands and
 * cheap to render.
 */
function projectAgp(
  buckets: AgpBucket[] | undefined,
  fromMs: number,
  toMs: number,
  bucketMinutes: number,
): AgpSample[] {
  if (!buckets || buckets.length === 0) return []
  const byBucket = new Map<number, AgpBucket>()
  for (const b of buckets) byBucket.set(b.bucketMinute, b)

  const tzOffsetMinutes = -new Date().getTimezoneOffset()
  const stepMs = 5 * 60_000
  const samples: AgpSample[] = []
  for (let t = Math.ceil(fromMs / stepMs) * stepMs; t <= toMs; t += stepMs) {
    const localMinuteOfDay = (((t / 60_000 + tzOffsetMinutes) % 1440) + 1440) % 1440
    const bucketMinute = Math.floor(localMinuteOfDay / bucketMinutes) * bucketMinutes
    const b = byBucket.get(bucketMinute)
    if (b) {
      samples.push({
        date: t,
        p5: b.p5,
        p25: b.p25,
        p50: b.p50,
        p75: b.p75,
        p95: b.p95,
      })
    }
  }
  return samples
}

function curveForLevel(level: number) {
  switch (level) {
    case 0:
      return curveLinear
    case 2:
      return curveCatmullRom
    case 3:
      return curveBasis
    case 1:
    default:
      return curveMonotoneX
  }
}

/** Centered moving average over an odd window. window=1 returns input. */
function smoothEntries(input: Entry[], window: number): Entry[] {
  if (window <= 1 || input.length < 3) return input
  const half = Math.floor(window / 2)
  const result: Entry[] = []
  for (let i = 0; i < input.length; i++) {
    let sum = 0
    let count = 0
    for (let j = Math.max(0, i - half); j <= Math.min(input.length - 1, i + half); j++) {
      const v = input[j].sgv
      if (v != null) {
        sum += v
        count++
      }
    }
    if (count > 0) {
      result.push({ ...input[i], sgv: sum / count })
    }
  }
  return result
}

export function BgChart(props: Props) {
  return (
    <ParentSize>
      {({ width, height }) => <BgChartInner {...props} width={width} height={height} />}
    </ParentSize>
  )
}

interface InnerProps extends Props {
  width: number
  height: number
}

interface TooltipData {
  entry: Entry
  treatments: Treatment[]
}

const bisectDate = bisector<Entry, number>((e) => e.date).left

function BgChartInner({
  width,
  height,
  entries,
  treatments = [],
  settings,
  hours,
  showLine = true,
  smoothing = 1,
  agpBuckets,
}: InnerProps) {
  const margin = { top: 16, right: 20, bottom: 36, left: 44 }
  const innerWidth = Math.max(0, width - margin.left - margin.right)
  const innerHeight = Math.max(0, height - margin.top - margin.bottom)

  const now = useNowMs(60_000)
  const xMin = now - hours * 3600_000

  // SGVs sorted oldest -> newest for line drawing & bisection
  const sgvs = useMemo(
    () =>
      entries
        .filter((e) => e.type === 'sgv' && e.sgv != null && e.date >= xMin)
        .slice()
        .sort((a, b) => a.date - b.date),
    [entries, xMin],
  )

  // Smoothed copy used only for the connecting line; raw points stay accurate
  const smoothedSgvs = useMemo(() => {
    const window = smoothing === 0 ? 1 : smoothing === 1 ? 3 : smoothing === 2 ? 5 : 7
    return smoothEntries(sgvs, window)
  }, [sgvs, smoothing])
  const lineCurve = curveForLevel(smoothing)

  const agpSamples = useMemo(() => projectAgp(agpBuckets, xMin, now, 15), [agpBuckets, xMin, now])

  const maxAgp = agpSamples.length > 0 ? Math.max(...agpSamples.map((s) => s.p95)) : 0
  const maxSgv = Math.max(settings.thresholds.bgHigh, ...sgvs.map((e) => e.sgv ?? 0), maxAgp)
  const yMaxMgdl = Math.ceil((maxSgv + 20) / 10) * 10
  const yMinMgdl = 40

  const xScale = useMemo(
    () => scaleTime({ domain: [xMin, now], range: [0, innerWidth] }),
    [xMin, now, innerWidth],
  )

  const yScale = useMemo(() => {
    const toDisplay = (mgdl: number) => (settings.units === 'mmol/l' ? mgdl / 18 : mgdl)
    return scaleLinear({
      domain: [toDisplay(yMinMgdl), toDisplay(yMaxMgdl)],
      range: [innerHeight, 0],
      nice: true,
    })
  }, [yMinMgdl, yMaxMgdl, innerHeight, settings.units])

  const toY = useCallback(
    (mgdl: number) => yScale(settings.units === 'mmol/l' ? mgdl / 18 : mgdl),
    [yScale, settings.units],
  )

  const targetTopY = toY(settings.thresholds.bgTargetTop)
  const targetBottomY = toY(settings.thresholds.bgTargetBottom)
  const urgentHighY = toY(settings.thresholds.bgHigh)
  const urgentLowY = toY(settings.thresholds.bgLow)
  const bandHeight = targetBottomY - targetTopY

  const { showTooltip, hideTooltip, tooltipData, tooltipLeft, tooltipTop, tooltipOpen } =
    useTooltip<TooltipData>()

  const handleHover = useCallback(
    (event: React.MouseEvent<SVGRectElement> | React.TouchEvent<SVGRectElement>) => {
      const point = localPoint(event)
      if (!point || sgvs.length === 0) return
      const x = point.x - margin.left
      const dateAtCursor = xScale.invert(x).getTime()

      const idx = bisectDate(sgvs, dateAtCursor, 1)
      const left = sgvs[idx - 1]
      const right = sgvs[idx]
      let nearest = left
      if (right && Math.abs(right.date - dateAtCursor) < Math.abs(left.date - dateAtCursor)) {
        nearest = right
      }
      if (!nearest || nearest.sgv == null) return

      // Find treatments within ±5 minutes of the nearest entry
      const nearTreatments = treatments.filter((t) => {
        const ts = Date.parse(t.created_at)
        return !Number.isNaN(ts) && Math.abs(ts - nearest.date) < 5 * 60_000
      })

      showTooltip({
        tooltipData: { entry: nearest, treatments: nearTreatments },
        tooltipLeft: xScale(nearest.date) + margin.left,
        tooltipTop: toY(nearest.sgv) + margin.top,
      })
    },
    [sgvs, treatments, margin.left, margin.top, xScale, toY, showTooltip],
  )

  return (
    <div style={{ position: 'relative', width, height }}>
      <svg width={width} height={height}>
        <Group left={margin.left} top={margin.top}>
          <GridRows scale={yScale} width={innerWidth} stroke="#2a2a2a" strokeDasharray="2,3" />

          {/* Target band */}
          <Bar
            x={0}
            y={targetTopY}
            width={innerWidth}
            height={bandHeight}
            fill="#4caf50"
            fillOpacity={0.1}
          />

          {/* AGP percentile bands (faded "typical you" backdrop) */}
          {agpSamples.length > 1 && (
            <>
              <Area<AgpSample>
                data={agpSamples}
                x={(d) => xScale(d.date)}
                y0={(d) => toY(d.p5)}
                y1={(d) => toY(d.p95)}
                curve={curveMonotoneX}
                fill="#90caf9"
                fillOpacity={0.1}
              />
              <Area<AgpSample>
                data={agpSamples}
                x={(d) => xScale(d.date)}
                y0={(d) => toY(d.p25)}
                y1={(d) => toY(d.p75)}
                curve={curveMonotoneX}
                fill="#90caf9"
                fillOpacity={0.18}
              />
              <LinePath<AgpSample>
                data={agpSamples}
                x={(d) => xScale(d.date)}
                y={(d) => toY(d.p50)}
                stroke="#90caf9"
                strokeWidth={1.5}
                strokeDasharray="3,3"
                strokeOpacity={0.6}
                curve={curveMonotoneX}
              />
            </>
          )}

          {/* Threshold guide lines */}
          <Line
            from={{ x: 0, y: urgentHighY }}
            to={{ x: innerWidth, y: urgentHighY }}
            stroke="#e53935"
            strokeOpacity={0.4}
            strokeDasharray="4,4"
          />
          <Line
            from={{ x: 0, y: urgentLowY }}
            to={{ x: innerWidth, y: urgentLowY }}
            stroke="#e53935"
            strokeOpacity={0.4}
            strokeDasharray="4,4"
          />
          <Line
            from={{ x: 0, y: targetTopY }}
            to={{ x: innerWidth, y: targetTopY }}
            stroke="#4caf50"
            strokeOpacity={0.5}
          />
          <Line
            from={{ x: 0, y: targetBottomY }}
            to={{ x: innerWidth, y: targetBottomY }}
            stroke="#4caf50"
            strokeOpacity={0.5}
          />

          {/* SGV connecting line (optional, smoothed) */}
          {showLine && smoothedSgvs.length > 1 && (
            <LinePath<Entry>
              data={smoothedSgvs}
              x={(d) => xScale(d.date)}
              y={(d) => toY(d.sgv ?? 0)}
              stroke="#e0e0e0"
              strokeWidth={3}
              strokeOpacity={0.9}
              strokeLinecap="round"
              strokeLinejoin="round"
              curve={lineCurve}
            />
          )}

          {/* SGV points */}
          {sgvs.map((e) => (
            <Circle
              key={e._id}
              cx={xScale(e.date)}
              cy={toY(e.sgv!)}
              r={3.5}
              fill={bgColor(e.sgv!, settings.thresholds)}
              stroke="#0a0a0a"
              strokeWidth={1}
            />
          ))}

          {/* Treatment markers */}
          {treatments.map((t) => {
            const ts = Date.parse(t.created_at)
            if (Number.isNaN(ts) || ts < xMin || ts > now) return null
            const x = xScale(ts)
            const isInsulin = (t.insulin ?? 0) > 0
            const isCarbs = (t.carbs ?? 0) > 0
            if (!isInsulin && !isCarbs) return null

            return (
              <g key={t._id}>
                {isCarbs && (
                  <>
                    <Line
                      from={{ x, y: 0 }}
                      to={{ x, y: innerHeight }}
                      stroke="#ffb74d"
                      strokeOpacity={0.25}
                      strokeWidth={1}
                      strokeDasharray="2,2"
                    />
                    <Circle
                      cx={x}
                      cy={innerHeight - 10}
                      r={9}
                      fill="#ffb74d"
                      stroke="#0a0a0a"
                      strokeWidth={1}
                    />
                    <text
                      x={x}
                      y={innerHeight - 7}
                      textAnchor="middle"
                      fontSize={11}
                      fill="#000"
                      fontWeight="700"
                    >
                      {Math.round(t.carbs ?? 0)}
                    </text>
                  </>
                )}
                {isInsulin && (
                  <>
                    <Line
                      from={{ x, y: 0 }}
                      to={{ x, y: innerHeight }}
                      stroke="#42a5f5"
                      strokeOpacity={0.25}
                      strokeWidth={1}
                      strokeDasharray="2,2"
                    />
                    <Circle
                      cx={x}
                      cy={isCarbs ? 10 : innerHeight - 10}
                      r={9}
                      fill="#42a5f5"
                      stroke="#0a0a0a"
                      strokeWidth={1}
                    />
                    <text
                      x={x}
                      y={(isCarbs ? 10 : innerHeight - 10) + 4}
                      textAnchor="middle"
                      fontSize={11}
                      fill="#fff"
                      fontWeight="700"
                    >
                      {(t.insulin ?? 0).toFixed(1)}
                    </text>
                  </>
                )}
              </g>
            )
          })}

          {/* Hover crosshair */}
          {tooltipOpen && tooltipData && (
            <>
              <Line
                from={{ x: xScale(tooltipData.entry.date), y: 0 }}
                to={{ x: xScale(tooltipData.entry.date), y: innerHeight }}
                stroke="#fff"
                strokeOpacity={0.4}
                strokeWidth={1}
                pointerEvents="none"
              />
              <Circle
                cx={xScale(tooltipData.entry.date)}
                cy={toY(tooltipData.entry.sgv!)}
                r={6}
                fill={bgColor(tooltipData.entry.sgv!, settings.thresholds)}
                stroke="#fff"
                strokeWidth={2}
                pointerEvents="none"
              />
            </>
          )}

          <AxisBottom
            top={innerHeight}
            scale={xScale}
            stroke="#666"
            tickStroke="#666"
            numTicks={Math.max(2, Math.floor(innerWidth / 90))}
            tickFormat={(d) =>
              (d as Date).toLocaleTimeString('en-GB', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: false,
              })
            }
            tickLabelProps={() => ({
              fill: '#bbb',
              fontSize: 13,
              fontWeight: 500,
              textAnchor: 'middle',
              dy: 4,
            })}
          />
          <AxisLeft
            scale={yScale}
            stroke="#666"
            tickStroke="#666"
            numTicks={5}
            tickLabelProps={() => ({
              fill: '#bbb',
              fontSize: 13,
              fontWeight: 500,
              textAnchor: 'end',
              dx: -6,
              dy: 4,
            })}
          />

          {/* Transparent overlay for hover handling */}
          <rect
            x={0}
            y={0}
            width={innerWidth}
            height={innerHeight}
            fill="transparent"
            onMouseMove={handleHover}
            onMouseLeave={hideTooltip}
            onTouchStart={handleHover}
            onTouchMove={handleHover}
            onTouchEnd={hideTooltip}
          />
        </Group>
      </svg>

      {tooltipOpen && tooltipData && tooltipLeft != null && tooltipTop != null && (
        <TooltipWithBounds
          left={tooltipLeft}
          top={tooltipTop - 12}
          style={{
            ...defaultStyles,
            background: 'rgba(20,20,20,0.95)',
            color: '#fff',
            border: '1px solid #444',
            borderRadius: 6,
            padding: '8px 12px',
            fontSize: 13,
            lineHeight: 1.4,
            pointerEvents: 'none',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
            <span
              style={{
                fontSize: 22,
                fontWeight: 700,
                color: bgColor(tooltipData.entry.sgv!, settings.thresholds),
              }}
            >
              {formatBg(tooltipData.entry.sgv!, settings.units)}
            </span>
            <span style={{ fontSize: 18 }}>
              {tooltipData.entry.direction
                ? (DIRECTION_ARROW[tooltipData.entry.direction] ?? '')
                : ''}
            </span>
            <span style={{ color: '#888', fontSize: 11 }}>{settings.units}</span>
          </div>
          <div style={{ color: '#bbb', fontSize: 11, marginTop: 2 }}>
            {new Date(tooltipData.entry.date).toLocaleTimeString('en-GB', {
              hour: '2-digit',
              minute: '2-digit',
              hour12: false,
            })}
          </div>
          {tooltipData.treatments.length > 0 && (
            <div style={{ marginTop: 6, paddingTop: 6, borderTop: '1px solid #444' }}>
              {tooltipData.treatments.map((t) => (
                <div key={t._id} style={{ fontSize: 11, color: '#ddd' }}>
                  {(t.insulin ?? 0) > 0 && `💉 ${(t.insulin ?? 0).toFixed(1)}U `}
                  {(t.carbs ?? 0) > 0 && `🍞 ${Math.round(t.carbs ?? 0)}g `}
                  <span style={{ color: '#888' }}>{t.eventType}</span>
                </div>
              ))}
            </div>
          )}
        </TooltipWithBounds>
      )}
    </div>
  )
}
