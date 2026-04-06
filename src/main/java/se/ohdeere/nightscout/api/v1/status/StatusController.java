package se.ohdeere.nightscout.api.v1.status;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import se.ohdeere.nightscout.NightscoutProperties;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StatusController {

	private final NightscoutProperties properties;

	StatusController(NightscoutProperties properties) {
		this.properties = properties;
	}

	@GetMapping({ "/api/v1/status", "/api/v1/status.json" })
	Map<String, Object> status() {
		List<String> enabledPlugins = Arrays.asList(this.properties.enable().split("\\s+"));
		return Map.of("status", "ok", "name", "nightscout", "version", "15.0.6-java", "serverTime",
				Instant.now().toString(), "serverTimeEpoch", Instant.now().toEpochMilli(), "apiEnabled", true,
				"careportalEnabled", true, "settings",
				Map.of("units", this.properties.units(), "timeFormat", this.properties.timeFormat(), "theme",
						this.properties.theme(), "language", this.properties.language(), "enable", enabledPlugins,
						"thresholds",
						Map.of("bgHigh", this.properties.thresholds().bgHigh(), "bgTargetTop",
								this.properties.thresholds().bgTargetTop(), "bgTargetBottom",
								this.properties.thresholds().bgTargetBottom(), "bgLow",
								this.properties.thresholds().bgLow())));
	}

}
