package se.ohdeere.nightscout.plugin.ar2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.service.admin.EffectiveSettings;
import se.ohdeere.nightscout.service.admin.EffectiveSettings.Thresholds;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import org.springframework.stereotype.Component;

/**
 * AR2 (Auto-Regressive order 2) predictive alert plugin. Predicts future glucose values
 * using the last two readings and extrapolates forward.
 */
@Component
public class Ar2Plugin {

	private static final int FORECAST_POINTS = 6;

	private static final int MINUTES_PER_POINT = 5;

	private final EntryRepository entryRepository;

	private final EffectiveSettings effective;

	public Ar2Plugin(EntryRepository entryRepository, EffectiveSettings effective) {
		this.entryRepository = entryRepository;
		this.effective = effective;
	}

	public Optional<PluginResult> calculate() {
		List<Entry> recent = this.entryRepository.findLatestByType("sgv", 2);
		if (recent.size() < 2 || recent.get(0).sgv() == null || recent.get(1).sgv() == null) {
			return Optional.empty();
		}

		double current = recent.get(0).sgv();
		double previous = recent.get(1).sgv();

		List<Double> forecast = predict(previous, current);

		Thresholds t = this.effective.thresholds();
		String level = "ok";
		for (double predicted : forecast) {
			if (predicted > t.bgHigh() || predicted < t.bgLow()) {
				level = "urgent";
				break;
			}
			if (predicted > t.bgTargetTop() || predicted < t.bgTargetBottom()) {
				level = "warn";
			}
		}

		return Optional.of(PluginResult.withData("ar2", "AR2", level.equals("ok") ? "" : "Predicted " + level,
				Map.of("forecast", forecast, "level", level)));
	}

	static List<Double> predict(double y1, double y2) {
		// Simple AR(2) extrapolation
		List<Double> points = new ArrayList<>();
		double prev = y1;
		double curr = y2;
		for (int i = 0; i < FORECAST_POINTS; i++) {
			double next = 2 * curr - prev; // linear extrapolation
			points.add(Math.round(next * 10.0) / 10.0);
			prev = curr;
			curr = next;
		}
		return points;
	}

}
