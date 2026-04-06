package se.ohdeere.nightscout.plugin.iob;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.storage.treatments.Treatment;
import se.ohdeere.nightscout.storage.treatments.TreatmentRepository;

import org.springframework.stereotype.Component;

/**
 * Insulin on Board (IOB) calculation. Uses a bilinear decay model with configurable DIA
 * (duration of insulin action, default 3 hours).
 */
@Component
public class IobPlugin {

	private static final double DEFAULT_DIA_HOURS = 3.0;

	private final TreatmentRepository treatmentRepository;

	public IobPlugin(TreatmentRepository treatmentRepository) {
		this.treatmentRepository = treatmentRepository;
	}

	public Optional<PluginResult> calculate() {
		Instant since = Instant.now().minus(Duration.ofHours((long) DEFAULT_DIA_HOURS));
		List<Treatment> treatments = this.treatmentRepository.findSince(since);

		double totalIob = 0.0;
		double totalActivity = 0.0;

		for (Treatment t : treatments) {
			if (t.insulin() != null && t.insulin() > 0) {
				double minutesAgo = Duration.between(t.createdAt(), Instant.now()).toMinutes();
				double[] result = bilinearIob(t.insulin(), minutesAgo, DEFAULT_DIA_HOURS * 60);
				totalIob += result[0];
				totalActivity += result[1];
			}
		}

		totalIob = Math.round(totalIob * 100.0) / 100.0;
		totalActivity = Math.round(totalActivity * 10000.0) / 10000.0;

		String display = totalIob + "U";
		return Optional
			.of(PluginResult.withData("iob", "IOB", display, Map.of("iob", totalIob, "activity", totalActivity)));
	}

	/**
	 * Bilinear insulin activity curve.
	 * @return [iob, activity]
	 */
	static double[] bilinearIob(double insulin, double minutesAgo, double diaMinutes) {
		if (minutesAgo < 0 || minutesAgo > diaMinutes) {
			return new double[] { 0, 0 };
		}

		double peak = diaMinutes / 3.0;
		double iob;
		double activity;

		if (minutesAgo < peak) {
			double x = minutesAgo / peak;
			iob = insulin * (1.0 - 0.5 * x);
			activity = insulin * (x / peak);
		}
		else {
			double x = (minutesAgo - peak) / (diaMinutes - peak);
			iob = insulin * 0.5 * (1.0 - x);
			activity = insulin * ((1.0 - x) / (diaMinutes - peak));
		}

		return new double[] { Math.max(0, iob), Math.max(0, activity) };
	}

}
