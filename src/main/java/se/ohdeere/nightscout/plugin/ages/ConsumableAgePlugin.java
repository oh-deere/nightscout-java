package se.ohdeere.nightscout.plugin.ages;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import se.ohdeere.nightscout.NightscoutProperties;
import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.storage.treatments.Treatment;
import se.ohdeere.nightscout.storage.treatments.TreatmentRepository;

import org.springframework.stereotype.Component;

/**
 * Consumable age plugins: CAGE (cannula), SAGE (sensor), IAGE (insulin), BAGE (battery).
 */
@Component
public class ConsumableAgePlugin {

	private final TreatmentRepository treatmentRepository;

	private final NightscoutProperties properties;

	public ConsumableAgePlugin(TreatmentRepository treatmentRepository, NightscoutProperties properties) {
		this.treatmentRepository = treatmentRepository;
		this.properties = properties;
	}

	public Optional<PluginResult> cage() {
		return calculateAge("cage", "CAGE", "Site Change", 48, 72);
	}

	public Optional<PluginResult> sage() {
		int warnHours = this.properties.sage().warn();
		int urgentHours = this.properties.sage().urgent();
		return calculateAge("sage", "SAGE", "Sensor Start", this.properties.sage().info(), warnHours, urgentHours);
	}

	public Optional<PluginResult> iage() {
		return calculateAge("iage", "IAGE", "Insulin Change", 72, 96);
	}

	public Optional<PluginResult> bage() {
		return calculateAge("bage", "BAGE", "Pump Battery Change", 336, 504);
	}

	private Optional<PluginResult> calculateAge(String name, String label, String eventType, int warnHours,
			int urgentHours) {
		return calculateAge(name, label, eventType, 0, warnHours, urgentHours);
	}

	private Optional<PluginResult> calculateAge(String name, String label, String eventType, int infoHours,
			int warnHours, int urgentHours) {
		Treatment latest = this.treatmentRepository.findLatestByEventType(eventType);
		if (latest == null) {
			return Optional.of(PluginResult.of(name, label, "N/A"));
		}

		long hours = Duration.between(latest.createdAt(), Instant.now()).toHours();
		long days = hours / 24;
		long remainingHours = hours % 24;

		String display = (days > 0) ? days + "d " + remainingHours + "h" : hours + "h";

		String level = "ok";
		if (hours >= urgentHours) {
			level = "urgent";
		}
		else if (hours >= warnHours) {
			level = "warn";
		}
		else if (infoHours > 0 && hours >= infoHours) {
			level = "info";
		}

		return Optional.of(PluginResult.withData(name, label, display,
				Map.of("hours", hours, "days", days, "level", level, "found", true)));
	}

}
