package se.ohdeere.nightscout.api.v1.status;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.ohdeere.nightscout.NightscoutProperties;
import se.ohdeere.nightscout.service.admin.EffectiveSettings;
import se.ohdeere.nightscout.service.admin.EffectiveSettings.Thresholds;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StatusController {

	private final NightscoutProperties properties;

	private final EffectiveSettings effective;

	private final boolean oauthEnabled;

	StatusController(NightscoutProperties properties, EffectiveSettings effective,
			@Value("${nightscout.oauth.enabled:false}") boolean oauthEnabled) {
		this.properties = properties;
		this.effective = effective;
		this.oauthEnabled = oauthEnabled;
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
		Thresholds thresholds = this.effective.thresholds();
		settings.put("units", this.effective.units());
		settings.put("timeFormat", this.effective.timeFormat());
		settings.put("nightMode", this.effective.nightMode());
		settings.put("theme", this.effective.theme());
		settings.put("language", this.effective.language());
		settings.put("showPlugins", this.properties.showPlugins());
		settings.put("enable", enabledPlugins);
		settings.put("alarmTypes", List.of(this.effective.alarmTypes()));
		settings.put("customTitle", this.effective.customTitle());
		settings.put("authDefaultRoles", this.effective.authDefaultRoles());
		settings.put("alarmTimeagoWarnMins", this.effective.alarmTimeagoWarnMins());
		settings.put("alarmTimeagoUrgentMins", this.effective.alarmTimeagoUrgentMins());
		settings.put("thresholds", Map.of("bgHigh", thresholds.bgHigh(), "bgTargetTop", thresholds.bgTargetTop(),
				"bgTargetBottom", thresholds.bgTargetBottom(), "bgLow", thresholds.bgLow()));

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
		// Frontend uses this to decide whether to render the "Sign in with OhDeere"
		// button on the login dialog. Each provider entry is { id, label, authorizeUrl }.
		if (this.oauthEnabled) {
			result.put("oauth", Map.of("enabled", true, "providers", List
				.of(Map.of("id", "ohdeere", "label", "OhDeere", "authorizeUrl", "/oauth2/authorization/ohdeere"))));
		}
		return result;
	}

}
