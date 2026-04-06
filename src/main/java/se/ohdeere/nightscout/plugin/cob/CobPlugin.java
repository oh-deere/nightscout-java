package se.ohdeere.nightscout.plugin.cob;

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
 * Carbs on Board (COB) calculation. Uses linear decay model with configurable absorption
 * rate.
 */
@Component
public class CobPlugin {

	private static final double DEFAULT_CARBS_HR = 20.0;

	private static final int ABSORPTION_HOURS = 8;

	private final TreatmentRepository treatmentRepository;

	public CobPlugin(TreatmentRepository treatmentRepository) {
		this.treatmentRepository = treatmentRepository;
	}

	public Optional<PluginResult> calculate() {
		Instant since = Instant.now().minus(Duration.ofHours(ABSORPTION_HOURS));
		List<Treatment> treatments = this.treatmentRepository.findSince(since);

		double totalCob = 0.0;

		for (Treatment t : treatments) {
			if (t.carbs() != null && t.carbs() > 0) {
				double minutesAgo = Duration.between(t.createdAt(), Instant.now()).toMinutes();
				double absorbed = (minutesAgo / 60.0) * DEFAULT_CARBS_HR;
				double remaining = Math.max(0, t.carbs() - absorbed);
				totalCob += remaining;
			}
		}

		int displayCob = (int) Math.round(totalCob);
		String display = displayCob + "g";
		return Optional
			.of(PluginResult.withData("cob", "COB", display, Map.of("cob", totalCob, "displayCob", displayCob)));
	}

}
