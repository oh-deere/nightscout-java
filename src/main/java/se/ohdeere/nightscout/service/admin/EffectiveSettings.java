package se.ohdeere.nightscout.service.admin;

import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import se.ohdeere.nightscout.NightscoutProperties;
import se.ohdeere.nightscout.NightscoutProperties.Thresholds;
import se.ohdeere.nightscout.storage.admin.RuntimeSettingRepository;

import org.springframework.stereotype.Service;

/**
 * Read-only view of the effective configuration: runtime overrides from the
 * {@code runtime_settings} table layered on top of the static
 * {@link NightscoutProperties} record.
 *
 * <p>
 * Each accessor reads the corresponding row by primary key. Lookups are cheap (single
 * indexed query) and there is no caching, so updates take effect immediately on the next
 * call.
 *
 * <p>
 * Override keys use dotted notation, e.g. {@code units}, {@code thresholds.bgHigh},
 * {@code alarmTimeagoWarnMins}.
 */
@Service
public class EffectiveSettings {

	private final NightscoutProperties properties;

	private final RuntimeSettingRepository settings;

	private final ObjectMapper mapper;

	public EffectiveSettings(NightscoutProperties properties, RuntimeSettingRepository settings, ObjectMapper mapper) {
		this.properties = properties;
		this.settings = settings;
		this.mapper = mapper;
	}

	public String units() {
		return string("units").orElse(this.properties.units());
	}

	public String customTitle() {
		return string("customTitle").orElse(this.properties.customTitle());
	}

	public int alarmTimeagoWarnMins() {
		return integer("alarmTimeagoWarnMins").orElse(this.properties.alarmTimeagoWarnMins());
	}

	public int alarmTimeagoUrgentMins() {
		return integer("alarmTimeagoUrgentMins").orElse(this.properties.alarmTimeagoUrgentMins());
	}

	public Thresholds thresholds() {
		Thresholds defaults = this.properties.thresholds();
		return new Thresholds(integer("thresholds.bgHigh").orElse(defaults.bgHigh()),
				integer("thresholds.bgTargetTop").orElse(defaults.bgTargetTop()),
				integer("thresholds.bgTargetBottom").orElse(defaults.bgTargetBottom()),
				integer("thresholds.bgLow").orElse(defaults.bgLow()));
	}

	/* ---------- helpers ---------- */

	private Optional<String> string(String key) {
		return read(key).map(node -> node.isTextual() ? node.asText() : node.toString());
	}

	private Optional<Integer> integer(String key) {
		return read(key).flatMap(node -> {
			if (node.isInt() || node.isLong()) {
				return Optional.of(node.asInt());
			}
			if (node.isTextual()) {
				try {
					return Optional.of(Integer.parseInt(node.asText()));
				}
				catch (NumberFormatException ex) {
					return Optional.empty();
				}
			}
			return Optional.empty();
		});
	}

	private Optional<JsonNode> read(String key) {
		return this.settings.findById(key).flatMap(row -> {
			if (row.value() == null || row.value().value() == null) {
				return Optional.empty();
			}
			try {
				return Optional.of(this.mapper.readTree(row.value().value()));
			}
			catch (Exception ex) {
				return Optional.empty();
			}
		});
	}

}
