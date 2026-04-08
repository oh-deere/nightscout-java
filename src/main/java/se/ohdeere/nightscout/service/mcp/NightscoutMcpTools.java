package se.ohdeere.nightscout.service.mcp;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import se.ohdeere.nightscout.service.admin.EffectiveSettings;
import se.ohdeere.nightscout.service.admin.EffectiveSettings.Thresholds;
import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.plugin.ages.ConsumableAgePlugin;
import se.ohdeere.nightscout.plugin.bgnow.BgNowPlugin;
import se.ohdeere.nightscout.plugin.cob.CobPlugin;
import se.ohdeere.nightscout.plugin.iob.IobPlugin;
import se.ohdeere.nightscout.service.entries.EntryService;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.treatments.Treatment;
import se.ohdeere.nightscout.storage.treatments.TreatmentRepository;

/**
 * Spring AI MCP tools that expose the Nightscout glucose data and plugins to LLM agents
 * (Claude Code, Claude Desktop, etc.).
 */
@Service
public class NightscoutMcpTools {

	private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();

	private static final double MGDL_TO_MMOL = 18.0;

	private final EntryService entryService;

	private final TreatmentRepository treatmentRepository;

	private final BgNowPlugin bgNowPlugin;

	private final IobPlugin iobPlugin;

	private final CobPlugin cobPlugin;

	private final ConsumableAgePlugin agePlugin;

	private final EffectiveSettings effective;

	public NightscoutMcpTools(EntryService entryService, TreatmentRepository treatmentRepository,
			BgNowPlugin bgNowPlugin, IobPlugin iobPlugin, CobPlugin cobPlugin, ConsumableAgePlugin agePlugin,
			EffectiveSettings effective) {
		this.entryService = entryService;
		this.treatmentRepository = treatmentRepository;
		this.bgNowPlugin = bgNowPlugin;
		this.iobPlugin = iobPlugin;
		this.cobPlugin = cobPlugin;
		this.agePlugin = agePlugin;
		this.effective = effective;
	}

	@McpTool(description = "Get the most recent glucose (SGV) reading from the continuous glucose monitor. "
			+ "Returns the value in both mg/dL and mmol/L, the trend direction, age in minutes, and a level classification "
			+ "(in-range, low, urgent low, high, urgent high) based on the configured thresholds.")
	public String getCurrentGlucose() {
		Entry current = this.entryService.findCurrent().orElse(null);
		if (current == null || current.sgv() == null) {
			return "No glucose readings available.";
		}
		long ageMins = Duration.ofMillis(System.currentTimeMillis() - current.dateMs()).toMinutes();
		String level = classify(current.sgv());
		return """
				Current glucose: %d mg/dL (%.1f mmol/L)
				Trend: %s
				Age: %d minutes
				Level: %s
				Device: %s
				Time: %s
				""".formatted(current.sgv(), current.sgv() / MGDL_TO_MMOL,
				current.direction() != null ? current.direction() : "unknown", ageMins, level,
				current.device() != null ? current.device() : "unknown", formatInstant(current.dateMs()));
	}

	@McpTool(description = "Get glucose readings within a time range. Returns up to 1000 readings sorted by time, "
			+ "with each value in mg/dL and mmol/L. Use ISO-8601 format for timestamps "
			+ "(e.g. 2026-04-07T08:00:00). Defaults to the last 6 hours if no parameters are given.")
	public String getGlucoseReadings(
			@McpToolParam(
					description = "Start of time range in ISO-8601 local time format "
							+ "(e.g. 2026-04-07T08:00:00). Defaults to 6 hours ago if omitted.",
					required = false) String from,
			@McpToolParam(description = "End of time range in ISO-8601 local time format. Defaults to now if omitted.",
					required = false) String to) {
		Instant fromInstant = parseOrDefault(from, Instant.now().minus(Duration.ofHours(6)));
		Instant toInstant = parseOrDefault(to, Instant.now());

		List<Entry> readings = this.entryService.findByDateRange(fromInstant.toEpochMilli(), toInstant.toEpochMilli())
			.stream()
			.filter(e -> "sgv".equals(e.type()) && e.sgv() != null)
			.sorted(Comparator.comparingLong(Entry::dateMs))
			.toList();

		if (readings.isEmpty()) {
			return "No glucose readings found between %s and %s.".formatted(fromInstant, toInstant);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Found %d readings between %s and %s:%n%n".formatted(readings.size(), formatInstant(fromInstant),
				formatInstant(toInstant)));
		for (Entry e : readings) {
			sb.append("%s — %d mg/dL (%.1f mmol/L) %s%n".formatted(formatInstant(e.dateMs()), e.sgv(),
					e.sgv() / MGDL_TO_MMOL, e.direction() != null ? e.direction() : ""));
		}
		return sb.toString();
	}

	@McpTool(description = "Get glucose statistics for a time period: average, min, max, std deviation, "
			+ "and time-in-range percentages (urgent low, low, in-range, high, urgent high). "
			+ "Useful for answering questions like 'how was Noah's glucose today?' or "
			+ "'what was the average over the last 7 days?'")
	public String getGlucoseStatistics(
			@McpToolParam(description = "Start of time period in ISO-8601 local time format. "
					+ "Defaults to 24 hours ago if omitted.", required = false) String from,
			@McpToolParam(description = "End of time period in ISO-8601 local time format. Defaults to now if omitted.",
					required = false) String to) {
		Instant fromInstant = parseOrDefault(from, Instant.now().minus(Duration.ofHours(24)));
		Instant toInstant = parseOrDefault(to, Instant.now());

		List<Entry> readings = this.entryService.findByDateRange(fromInstant.toEpochMilli(), toInstant.toEpochMilli())
			.stream()
			.filter(e -> "sgv".equals(e.type()) && e.sgv() != null)
			.toList();

		if (readings.isEmpty()) {
			return "No glucose readings found between %s and %s.".formatted(formatInstant(fromInstant),
					formatInstant(toInstant));
		}

		int n = readings.size();
		double sum = 0;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		long minTs = 0;
		long maxTs = 0;
		int urgentLow = 0;
		int low = 0;
		int inRange = 0;
		int high = 0;
		int urgentHigh = 0;

		for (Entry e : readings) {
			int v = e.sgv();
			sum += v;
			if (v < min) {
				min = v;
				minTs = e.dateMs();
			}
			if (v > max) {
				max = v;
				maxTs = e.dateMs();
			}
			if (v <= this.effective.thresholds().bgLow()) {
				urgentLow++;
			}
			else if (v < this.effective.thresholds().bgTargetBottom()) {
				low++;
			}
			else if (v > this.effective.thresholds().bgHigh()) {
				urgentHigh++;
			}
			else if (v > this.effective.thresholds().bgTargetTop()) {
				high++;
			}
			else {
				inRange++;
			}
		}
		double mean = sum / n;
		double variance = readings.stream().mapToDouble(e -> Math.pow(e.sgv() - mean, 2)).sum() / n;
		double sd = Math.sqrt(variance);
		double cv = (sd / mean) * 100;
		double a1c = 3.31 + 0.02392 * mean;

		return """
				Glucose statistics from %s to %s

				Readings: %d
				Mean: %.0f mg/dL (%.1f mmol/L)
				Std deviation: %.0f mg/dL (CV %.0f%%)
				Min: %d mg/dL (%.1f mmol/L) at %s
				Max: %d mg/dL (%.1f mmol/L) at %s
				Estimated A1C (GMI): %.1f%%

				Time in Range (target %d-%d mg/dL):
				  In range: %.1f%%
				  Low: %.1f%%
				  Urgent low: %.1f%%
				  High: %.1f%%
				  Urgent high: %.1f%%
				""".formatted(formatInstant(fromInstant), formatInstant(toInstant), n, mean, mean / MGDL_TO_MMOL, sd,
				cv, min, min / MGDL_TO_MMOL, formatInstant(minTs), max, max / MGDL_TO_MMOL, formatInstant(maxTs), a1c,
				this.effective.thresholds().bgTargetBottom(), this.effective.thresholds().bgTargetTop(),
				pct(inRange, n), pct(low, n), pct(urgentLow, n), pct(high, n), pct(urgentHigh, n));
	}

	@McpTool(description = "Get treatments (insulin doses, carb entries, sensor changes, etc.) within a time range. "
			+ "Returns the most recent up to 100 treatments. Use ISO-8601 format for timestamps. "
			+ "Defaults to the last 24 hours.")
	public String getTreatments(@McpToolParam(description = "Start of time range in ISO-8601 local time format. "
			+ "Defaults to 24 hours ago if omitted.", required = false) String from) {
		Instant fromInstant = parseOrDefault(from, Instant.now().minus(Duration.ofHours(24)));
		List<Treatment> treatments = this.treatmentRepository.findSince(fromInstant);
		if (treatments.isEmpty()) {
			return "No treatments found since " + formatInstant(fromInstant) + ".";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Found %d treatments since %s:%n%n".formatted(treatments.size(), formatInstant(fromInstant)));
		for (Treatment t : treatments.stream().limit(100).toList()) {
			sb.append("%s — %s".formatted(formatInstant(t.createdAt()), t.eventType()));
			if (t.insulin() != null && t.insulin() > 0) {
				sb.append(" · insulin %.1fU".formatted(t.insulin()));
			}
			if (t.carbs() != null && t.carbs() > 0) {
				sb.append(" · carbs %.0fg".formatted(t.carbs()));
			}
			if (t.notes() != null && !t.notes().isBlank()) {
				sb.append(" · ").append(t.notes());
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	@McpTool(
			description = "Get the current diabetes management snapshot: insulin on board (IOB), carbs on board (COB), "
					+ "sensor age (SAGE), and cannula/site age (CAGE). Useful for one-shot 'how is Noah right now?' questions.")
	public String getCurrentSnapshot() {
		StringBuilder sb = new StringBuilder();
		this.bgNowPlugin.calculate().ifPresent(r -> appendPlugin(sb, "BG", r));
		this.iobPlugin.calculate().ifPresent(r -> appendPlugin(sb, "IOB", r));
		this.cobPlugin.calculate().ifPresent(r -> appendPlugin(sb, "COB", r));
		this.agePlugin.cage().ifPresent(r -> appendPlugin(sb, "Cannula age", r));
		this.agePlugin.sage().ifPresent(r -> appendPlugin(sb, "Sensor age", r));
		this.agePlugin.iage().ifPresent(r -> appendPlugin(sb, "Insulin age", r));
		if (sb.length() == 0) {
			return "No data available.";
		}
		return sb.toString();
	}

	private static void appendPlugin(StringBuilder sb, String label, PluginResult r) {
		sb.append(label).append(": ").append(r.value());
		Map<String, Object> data = r.data();
		if (data != null) {
			Object level = data.get("level");
			if (level != null && !"ok".equals(level)) {
				sb.append(" (").append(level).append(')');
			}
		}
		sb.append('\n');
	}

	private String classify(int sgv) {
		if (sgv >= this.effective.thresholds().bgHigh()) {
			return "urgent high";
		}
		if (sgv > this.effective.thresholds().bgTargetTop()) {
			return "high";
		}
		if (sgv <= this.effective.thresholds().bgLow()) {
			return "urgent low";
		}
		if (sgv < this.effective.thresholds().bgTargetBottom()) {
			return "low";
		}
		return "in range";
	}

	private static double pct(int count, int total) {
		return total == 0 ? 0 : (count * 100.0) / total;
	}

	private static Instant parseOrDefault(String iso, Instant fallback) {
		if (iso == null || iso.isBlank()) {
			return fallback;
		}
		try {
			return LocalDateTime.parse(iso).atZone(LOCAL_ZONE).toInstant();
		}
		catch (DateTimeParseException ex) {
			try {
				return Instant.parse(iso);
			}
			catch (DateTimeParseException ignore) {
				return fallback;
			}
		}
	}

	private static String formatInstant(long epochMs) {
		return formatInstant(Instant.ofEpochMilli(epochMs));
	}

	private static String formatInstant(Instant instant) {
		return instant.atZone(LOCAL_ZONE).toLocalDateTime().toString();
	}

}
