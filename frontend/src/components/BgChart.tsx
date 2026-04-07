import { useMemo } from 'react'
import { Group } from '@visx/group'
import { scaleLinear, scaleTime } from '@visx/scale'
import { AxisBottom, AxisLeft } from '@visx/axis'
import { GridRows } from '@visx/grid'
import { Circle, Bar, Line } from '@visx/shape'
import { ParentSize } from '@visx/responsive'
import type { Entry, NightscoutSettings, Treatment } from '../types/nightscout'
import { bgColor } from '../theme/theme'
import { toDisplayBg } from '../utils/units'

interface Props {
  entries: Entry[]
  treatments?: Treatment[]
  settings: NightscoutSettings
  hours: number
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

function BgChartInner({ width, height, entries, treatments = [], settings, hours }: InnerProps) {
  const margin = { top: 10, right: 16, bottom: 28, left: 36 }
  const innerWidth = Math.max(0, width - margin.left - margin.right)
  const innerHeight = Math.max(0, height - margin.top - margin.bottom)

  const now = Date.now()
  const xMin = now - hours * 3600_000

  const sgvs = entries.filter((e) => e.type === 'sgv' && e.sgv != null)
  const maxSgv = Math.max(settings.thresholds.bgHigh, ...sgvs.map((e) => e.sgv ?? 0))
  const yMaxMgdl = Math.ceil((maxSgv + 20) / 10) * 10
  const yMinMgdl = 40

  const xScale = useMemo(
    () => scaleTime({ domain: [xMin, now], range: [0, innerWidth] }),
    [xMin, now, innerWidth],
  )

  const yScale = useMemo(
    () =>
      scaleLinear({
        domain: [toDisplayBg(yMinMgdl, settings.units), toDisplayBg(yMaxMgdl, settings.units)],
        range: [innerHeight, 0],
        nice: true,
      }),
    [yMinMgdl, yMaxMgdl, innerHeight, settings.units],
  )

  const targetTop = toDisplayBg(settings.thresholds.bgTargetTop, settings.units)
  const targetBottom = toDisplayBg(settings.thresholds.bgTargetBottom, settings.units)
  const bandY = yScale(targetTop)
  const bandHeight = yScale(targetBottom) - yScale(targetTop)

  return (
    <svg width={width} height={height}>
      <Group left={margin.left} top={margin.top}>
        <GridRows scale={yScale} width={innerWidth} stroke="#222" strokeDasharray="2,2" />
        <Bar
          x={0}
          y={bandY}
          width={innerWidth}
          height={bandHeight}
          fill="#4caf50"
          fillOpacity={0.08}
        />

        {/* SGV points */}
        {sgvs.map((e) => {
          if (e.sgv == null || e.date < xMin) return null
          return (
            <Circle
              key={e._id}
              cx={xScale(e.date)}
              cy={yScale(toDisplayBg(e.sgv, settings.units))}
              r={2.5}
              fill={bgColor(e.sgv, settings.thresholds)}
            />
          )
        })}

        {/* Treatment markers — drawn as vertical lines from baseline with icons */}
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
                  <Circle cx={x} cy={innerHeight - 8} r={6} fill="#ffb74d" />
                  <text
                    x={x}
                    y={innerHeight - 5}
                    textAnchor="middle"
                    fontSize={9}
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
                  <Circle cx={x} cy={isCarbs ? 8 : innerHeight - 8} r={6} fill="#42a5f5" />
                  <text
                    x={x}
                    y={(isCarbs ? 8 : innerHeight - 8) + 3}
                    textAnchor="middle"
                    fontSize={9}
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

        <AxisBottom
          top={innerHeight}
          scale={xScale}
          stroke="#444"
          tickStroke="#444"
          numTicks={Math.max(2, Math.floor(innerWidth / 80))}
          tickLabelProps={() => ({
            fill: '#888',
            fontSize: 10,
            textAnchor: 'middle',
          })}
        />
        <AxisLeft
          scale={yScale}
          stroke="#444"
          tickStroke="#444"
          numTicks={5}
          tickLabelProps={() => ({
            fill: '#888',
            fontSize: 10,
            textAnchor: 'end',
            dx: -4,
            dy: 3,
          })}
        />
      </Group>
    </svg>
  )
}
