package se.ohdeere.nightscout.plugin.bgnow;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.ohdeere.nightscout.NightscoutProperties;
import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import org.springframework.stereotype.Component;

@Component
public class BgNowPlugin {

	private final EntryRepository entryRepository;

	private final NightscoutProperties properties;

	public BgNowPlugin(EntryRepository entryRepository, NightscoutProperties properties) {
		this.entryRepository = entryRepository;
		this.properties = properties;
	}

	public Optional<PluginResult> calculate() {
		List<Entry> recent = this.entryRepository.findLatestByType("sgv", 2);
		if (recent.isEmpty()) {
			return Optional.empty();
		}

		Entry current = recent.getFirst();
		int sgv = (current.sgv() != null) ? current.sgv() : 0;
		String direction = (current.direction() != null) ? current.direction() : "";
		String arrow = directionArrow(direction);

		// Calculate delta
		Integer delta = null;
		if (recent.size() >= 2 && recent.get(1).sgv() != null) {
			delta = sgv - recent.get(1).sgv();
		}

		// Stale detection
		long ageMs = System.currentTimeMillis() - current.dateMs();
		long ageMins = Duration.ofMillis(ageMs).toMinutes();
		String level = classifyLevel(sgv, ageMins);

		String displayValue = formatBg(sgv) + " " + arrow;
		String deltaStr = (delta != null) ? formatDelta(delta) : "";

		return Optional.of(PluginResult.withData("bgnow", "BG Now", displayValue,
				Map.of("sgv", sgv, "direction", direction, "delta", (delta != null) ? delta : 0, "deltaDisplay",
						deltaStr, "ageMins", ageMins, "level", level, "arrow", arrow, "displayBg", formatBg(sgv))));
	}

	private String classifyLevel(int sgv, long ageMins) {
		if (ageMins > this.properties.alarmTimeagoUrgentMins()) {
			return "urgent";
		}
		if (ageMins > this.properties.alarmTimeagoWarnMins()) {
			return "warn";
		}
		if (sgv > this.properties.thresholds().bgHigh() || sgv < this.properties.thresholds().bgLow()) {
			return "urgent";
		}
		if (sgv > this.properties.thresholds().bgTargetTop() || sgv < this.properties.thresholds().bgTargetBottom()) {
			return "warn";
		}
		return "ok";
	}

	private String formatBg(int mgdl) {
		double display = this.properties.toDisplayUnits(mgdl);
		if ("mmol/l".equalsIgnoreCase(this.properties.units()) || "mmol".equalsIgnoreCase(this.properties.units())) {
			return String.valueOf(display);
		}
		return String.valueOf((int) display);
	}

	private String formatDelta(int deltaMgdl) {
		String prefix = (deltaMgdl > 0) ? "+" : "";
		if ("mmol/l".equalsIgnoreCase(this.properties.units()) || "mmol".equalsIgnoreCase(this.properties.units())) {
			double mmol = Math.round(deltaMgdl / 18.0 * 10.0) / 10.0;
			return prefix + mmol;
		}
		return prefix + deltaMgdl;
	}

	static String directionArrow(String direction) {
		return switch (direction) {
			case "DoubleUp" -> "\u21c8";
			case "SingleUp" -> "\u2191";
			case "FortyFiveUp" -> "\u2197";
			case "Flat" -> "\u2192";
			case "FortyFiveDown" -> "\u2198";
			case "SingleDown" -> "\u2193";
			case "DoubleDown" -> "\u21ca";
			case "NOT COMPUTABLE" -> "-";
			case "RATE OUT OF RANGE" -> "\u21d5";
			default -> "";
		};
	}

}
