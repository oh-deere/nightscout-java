package se.ohdeere.nightscout.plugin.bgnow;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.service.admin.EffectiveSettings;
import se.ohdeere.nightscout.service.admin.EffectiveSettings.Thresholds;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import org.springframework.stereotype.Component;

@Component
public class BgNowPlugin {

	private final EntryRepository entryRepository;

	private final EffectiveSettings effective;

	public BgNowPlugin(EntryRepository entryRepository, EffectiveSettings effective) {
		this.entryRepository = entryRepository;
		this.effective = effective;
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
		if (ageMins > this.effective.alarmTimeagoUrgentMins()) {
			return "urgent";
		}
		if (ageMins > this.effective.alarmTimeagoWarnMins()) {
			return "warn";
		}
		Thresholds t = this.effective.thresholds();
		if (sgv > t.bgHigh() || sgv < t.bgLow()) {
			return "urgent";
		}
		if (sgv > t.bgTargetTop() || sgv < t.bgTargetBottom()) {
			return "warn";
		}
		return "ok";
	}

	private String formatBg(int mgdl) {
		if (this.effective.isMmol()) {
			return String.valueOf(this.effective.toDisplayUnits(mgdl));
		}
		return String.valueOf(mgdl);
	}

	private String formatDelta(int deltaMgdl) {
		String prefix = (deltaMgdl > 0) ? "+" : "";
		if (this.effective.isMmol()) {
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
