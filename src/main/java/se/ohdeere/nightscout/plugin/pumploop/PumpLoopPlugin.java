package se.ohdeere.nightscout.plugin.pumploop;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.devicestatus.DeviceStatus;
import se.ohdeere.nightscout.storage.devicestatus.DeviceStatusRepository;

import org.springframework.stereotype.Component;

/**
 * Surfaces pump and loop status from the most recent {@code device_status} row.
 *
 * <p>
 * Three flavours of payload show up in the wild:
 * <ul>
 * <li>OpenAPS / AAPS — top-level {@code openaps.suggested|enacted|iob} + {@code pump}
 * sub-objects</li>
 * <li>Loop (LoopKit) — top-level {@code loop.iob|cob|predicted|enacted}</li>
 * <li>Bare pump uploaders — only {@code pump.reservoir|battery|status}</li>
 * </ul>
 *
 * <p>
 * Each type is parsed defensively (best-effort field extraction) and merged into a single
 * result so the frontend can show pump reservoir + last-loop-time + a stale-loop warning
 * regardless of which uploader is in use.
 */
@Component
public class PumpLoopPlugin {

	private static final long STALE_LOOP_MINUTES = 15;

	private final DeviceStatusRepository repo;

	private final ObjectMapper mapper;

	public PumpLoopPlugin(DeviceStatusRepository repo, ObjectMapper mapper) {
		this.repo = repo;
		this.mapper = mapper;
	}

	public Optional<PluginResult> calculate() {
		DeviceStatus latest = this.repo.findCurrent();
		if (latest == null) {
			return Optional.empty();
		}

		Map<String, Object> data = new LinkedHashMap<>();

		// Pump fields (manufacturer, reservoir, battery)
		JsonNode pump = parse(latest.pump());
		if (pump != null) {
			putIfPresent(data, "pumpReservoir",
					pump.path("reservoir").isMissingNode() ? null : pump.path("reservoir").asDouble());
			putIfPresent(data, "pumpManufacturer", pump.path("manufacturer").asText(null));
			putIfPresent(data, "pumpModel", pump.path("model").asText(null));
			JsonNode battery = pump.path("battery");
			if (!battery.isMissingNode()) {
				putIfPresent(data, "pumpBatteryPercent",
						battery.path("percent").isMissingNode() ? null : battery.path("percent").asInt());
				putIfPresent(data, "pumpBatteryVoltage",
						battery.path("voltage").isMissingNode() ? null : battery.path("voltage").asDouble());
			}
		}

		// Loop / OpenAPS state
		JsonNode loop = parse(latest.loop());
		JsonNode openaps = parse(latest.openaps());
		Instant loopTimestamp = null;
		String source = null;

		if (loop != null && !loop.isEmpty()) {
			source = "loop";
			loopTimestamp = parseInstant(loop.path("timestamp").asText(null));
			putIfPresent(data, "loopName", loop.path("name").asText(null));
			putIfPresent(data, "loopVersion", loop.path("version").asText(null));
			JsonNode iob = loop.path("iob");
			if (!iob.isMissingNode()) {
				putIfPresent(data, "loopIob", iob.path("iob").isMissingNode() ? null : iob.path("iob").asDouble());
			}
			JsonNode cob = loop.path("cob");
			if (!cob.isMissingNode()) {
				putIfPresent(data, "loopCob", cob.path("cob").isMissingNode() ? null : cob.path("cob").asDouble());
			}
		}
		else if (openaps != null && !openaps.isEmpty()) {
			source = "openaps";
			JsonNode suggested = openaps.path("suggested");
			JsonNode enacted = openaps.path("enacted");
			loopTimestamp = parseInstant(suggested.path("timestamp").asText(null));
			if (loopTimestamp == null) {
				loopTimestamp = parseInstant(enacted.path("timestamp").asText(null));
			}
			JsonNode iob = openaps.path("iob");
			if (!iob.isMissingNode()) {
				putIfPresent(data, "loopIob", iob.path("iob").isMissingNode() ? null : iob.path("iob").asDouble());
			}
			putIfPresent(data, "openapsReason", suggested.path("reason").asText(null));
			putIfPresent(data, "openapsEventualBg",
					suggested.path("eventualBG").isMissingNode() ? null : suggested.path("eventualBG").asInt());
		}

		if (source != null) {
			data.put("source", source);
		}
		if (loopTimestamp != null) {
			data.put("loopTimestamp", loopTimestamp.toString());
			long ageMinutes = Duration.between(loopTimestamp, Instant.now()).toMinutes();
			data.put("loopAgeMinutes", ageMinutes);
			data.put("loopStale", ageMinutes >= STALE_LOOP_MINUTES);
		}

		if (data.isEmpty()) {
			return Optional.empty();
		}

		String label = (source != null) ? source.toUpperCase() : "Pump";
		String value = data.containsKey("loopIob") ? "IOB " + data.get("loopIob") + "U"
				: (data.containsKey("pumpReservoir") ? "Reservoir " + data.get("pumpReservoir") + "U" : "ok");
		return Optional.of(PluginResult.withData("pump", label, value, data));
	}

	private JsonNode parse(JsonValue value) {
		if (value == null || value.value() == null || value.value().isBlank()) {
			return null;
		}
		try {
			return this.mapper.readTree(value.value());
		}
		catch (Exception ex) {
			return null;
		}
	}

	private static Instant parseInstant(String iso) {
		if (iso == null || iso.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(iso);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private static void putIfPresent(Map<String, Object> map, String key, Object value) {
		if (value != null) {
			map.put(key, value);
		}
	}

}
