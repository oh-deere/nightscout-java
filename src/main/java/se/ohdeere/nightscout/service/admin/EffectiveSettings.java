package se.ohdeere.nightscout.service.admin;

import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import se.ohdeere.nightscout.storage.admin.RuntimeSettingRepository;

import org.springframework.stereotype.Service;

/**
 * Read-only view of the effective configuration: runtime overrides from the
 * {@code runtime_settings} table layered on top of compiled-in defaults.
 *
 * <p>
 * Only three knobs remain in static configuration ({@code application.properties} / env
 * vars): {@code nightscout.api-secret}, {@code nightscout.enable}, and
 * {@code nightscout.show-plugins}. Everything else lives here, with sensible defaults
 * baked in and editable through the admin UI.
 *
 * <p>
 * Each accessor reads the corresponding row by primary key. Lookups are cheap (single
 * indexed query) and there is no caching, so updates take effect immediately on the next
 * call.
 *
 * <p>
 * Override keys use dotted notation, e.g. {@code units}, {@code thresholds.bgHigh},
 * {@code sage.warn}.
 */
@Service
public class EffectiveSettings {

	/*
	 * ---------- compiled-in defaults (replace the env-driven NightscoutProperties)
	 * ----------
	 */

	private static final String DEFAULT_UNITS = "mmol/l";

	private static final int DEFAULT_TIME_FORMAT = 24;

	private static final String DEFAULT_THEME = "default";

	private static final String DEFAULT_LANGUAGE = "en";

	private static final String DEFAULT_CUSTOM_TITLE = "Nightscout";

	private static final String DEFAULT_ALARM_TYPES = "simple";

	private static final String DEFAULT_AUTH_DEFAULT_ROLES = "denied";

	private static final boolean DEFAULT_NIGHT_MODE = false;

	private static final boolean DEFAULT_DEVICESTATUS_ADVANCED = false;

	private static final int DEFAULT_BOLUS_RENDER_OVER = 1;

	private static final int DEFAULT_ALARM_TIMEAGO_WARN_MINS = 15;

	private static final int DEFAULT_ALARM_TIMEAGO_URGENT_MINS = 30;

	private static final int DEFAULT_BG_HIGH = 260;

	private static final int DEFAULT_BG_TARGET_TOP = 180;

	private static final int DEFAULT_BG_TARGET_BOTTOM = 80;

	private static final int DEFAULT_BG_LOW = 55;

	private static final int DEFAULT_DELTA_WARN_MGDL = 15;

	private static final int DEFAULT_DELTA_URGENT_MGDL = 25;

	private static final int DEFAULT_SAGE_INFO_HOURS = 240;

	private static final int DEFAULT_SAGE_WARN_HOURS = 312;

	private static final int DEFAULT_SAGE_URGENT_HOURS = 336;

	public record Thresholds(int bgHigh, int bgTargetTop, int bgTargetBottom, int bgLow) {
	}

	public record Sage(int info, int warn, int urgent) {
	}

	private final RuntimeSettingRepository settings;

	private final ObjectMapper mapper;

	public EffectiveSettings(RuntimeSettingRepository settings, ObjectMapper mapper) {
		this.settings = settings;
		this.mapper = mapper;
	}

	/* ---------- string accessors ---------- */

	public String units() {
		return string("units").orElse(DEFAULT_UNITS);
	}

	public String customTitle() {
		return string("customTitle").orElse(DEFAULT_CUSTOM_TITLE);
	}

	public String theme() {
		return string("theme").orElse(DEFAULT_THEME);
	}

	public String language() {
		return string("language").orElse(DEFAULT_LANGUAGE);
	}

	public String alarmTypes() {
		return string("alarmTypes").orElse(DEFAULT_ALARM_TYPES);
	}

	public String authDefaultRoles() {
		return string("authDefaultRoles").orElse(DEFAULT_AUTH_DEFAULT_ROLES);
	}

	/* ---------- numeric accessors ---------- */

	public int timeFormat() {
		return integer("timeFormat").orElse(DEFAULT_TIME_FORMAT);
	}

	public int bolusRenderOver() {
		return integer("bolusRenderOver").orElse(DEFAULT_BOLUS_RENDER_OVER);
	}

	public int alarmTimeagoWarnMins() {
		return integer("alarmTimeagoWarnMins").orElse(DEFAULT_ALARM_TIMEAGO_WARN_MINS);
	}

	public int alarmTimeagoUrgentMins() {
		return integer("alarmTimeagoUrgentMins").orElse(DEFAULT_ALARM_TIMEAGO_URGENT_MINS);
	}

	public int deltaWarnMgdl() {
		return integer("delta.warn").orElse(DEFAULT_DELTA_WARN_MGDL);
	}

	public int deltaUrgentMgdl() {
		return integer("delta.urgent").orElse(DEFAULT_DELTA_URGENT_MGDL);
	}

	/* ---------- boolean accessors ---------- */

	public boolean nightMode() {
		return bool("nightMode").orElse(DEFAULT_NIGHT_MODE);
	}

	public boolean devicestatusAdvanced() {
		return bool("devicestatusAdvanced").orElse(DEFAULT_DEVICESTATUS_ADVANCED);
	}

	/* ---------- composite accessors ---------- */

	public Thresholds thresholds() {
		return new Thresholds(integer("thresholds.bgHigh").orElse(DEFAULT_BG_HIGH),
				integer("thresholds.bgTargetTop").orElse(DEFAULT_BG_TARGET_TOP),
				integer("thresholds.bgTargetBottom").orElse(DEFAULT_BG_TARGET_BOTTOM),
				integer("thresholds.bgLow").orElse(DEFAULT_BG_LOW));
	}

	public Sage sage() {
		return new Sage(integer("sage.info").orElse(DEFAULT_SAGE_INFO_HOURS),
				integer("sage.warn").orElse(DEFAULT_SAGE_WARN_HOURS),
				integer("sage.urgent").orElse(DEFAULT_SAGE_URGENT_HOURS));
	}

	/* ---------- display helpers ---------- */

	/** Convert mg/dL to the configured display units. */
	public double toDisplayUnits(int mgdl) {
		String u = units();
		if ("mmol/l".equalsIgnoreCase(u) || "mmol".equalsIgnoreCase(u)) {
			return Math.round(mgdl / 18.0 * 10.0) / 10.0;
		}
		return mgdl;
	}

	public boolean isMmol() {
		String u = units();
		return "mmol/l".equalsIgnoreCase(u) || "mmol".equalsIgnoreCase(u);
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

	private Optional<Boolean> bool(String key) {
		return read(key).flatMap(node -> {
			if (node.isBoolean()) {
				return Optional.of(node.asBoolean());
			}
			if (node.isTextual()) {
				return Optional.of(Boolean.parseBoolean(node.asText()));
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
