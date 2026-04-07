package se.ohdeere.nightscout.api.v1.status;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
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

	@GetMapping("/api/versions")
	Map<String, Object> versions() {
		return Map.of("versions", List.of("1", "2", "3"), "current", "3");
	}

	/**
	 * Loop uses this as a 200 auth probe before doing real work. Upstream Nightscout
	 * gates it on {@code authorization:debug:test}; we accept any caller with read
	 * access.
	 */
	@GetMapping("/api/v1/experiments/test")
	Map<String, Object> experimentsTest() {
		se.ohdeere.nightscout.api.auth.AuthHelper.requirePermission("entries", "read");
		return Map.of("status", "ok");
	}

	@GetMapping({ "/api/v1/status", "/api/v1/status.json" })
	Map<String, Object> status() {
		List<String> enabledPlugins = Arrays.asList(this.properties.enable().split("\\s+"));
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("units", this.properties.units());
		settings.put("timeFormat", this.properties.timeFormat());
		settings.put("nightMode", this.properties.nightMode());
		settings.put("theme", this.properties.theme());
		settings.put("language", this.properties.language());
		settings.put("showPlugins", this.properties.showPlugins());
		settings.put("enable", enabledPlugins);
		settings.put("alarmTypes", List.of(this.properties.alarmTypes()));
		settings.put("customTitle", this.properties.customTitle());
		settings.put("authDefaultRoles", this.properties.authDefaultRoles());
		settings.put("alarmTimeagoWarnMins", this.properties.alarmTimeagoWarnMins());
		settings.put("alarmTimeagoUrgentMins", this.properties.alarmTimeagoUrgentMins());
		settings.put("thresholds",
				Map.of("bgHigh", this.properties.thresholds().bgHigh(), "bgTargetTop",
						this.properties.thresholds().bgTargetTop(), "bgTargetBottom",
						this.properties.thresholds().bgTargetBottom(), "bgLow", this.properties.thresholds().bgLow()));

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("runtimeState", "loaded");
		result.put("status", "ok");
		result.put("name", "nightscout");
		result.put("version", "15.0.6-java");
		result.put("serverTime", Instant.now().toString());
		result.put("serverTimeEpoch", Instant.now().toEpochMilli());
		result.put("apiEnabled", true);
		result.put("careportalEnabled", enabledPlugins.contains("careportal"));
		result.put("boluscalcEnabled", enabledPlugins.contains("boluscalc"));
		result.put("settings", settings);
		return result;
	}

}
