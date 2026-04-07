package se.ohdeere.nightscout.api.v3;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.devicestatus.DeviceStatus;
import se.ohdeere.nightscout.storage.devicestatus.DeviceStatusRepository;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class V3DeviceStatusController {

	private final DeviceStatusRepository repository;

	private final ObjectMapper objectMapper;

	V3DeviceStatusController(DeviceStatusRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/api/v3/devicestatus")
	List<Map<String, Object>> list(@RequestParam(defaultValue = "100") int limit) {
		AuthHelper.requirePermission("devicestatus", "read");
		return this.repository.findLatest(Math.min(limit, 1000)).stream().map(this::toV3Map).toList();
	}

	@GetMapping("/api/v3/devicestatus/history/{lastModified}")
	List<Map<String, Object>> history(@PathVariable long lastModified) {
		AuthHelper.requirePermission("devicestatus", "read");
		return this.repository.findLatest(1000)
			.stream()
			.filter(ds -> ds.createdAt().toEpochMilli() > lastModified)
			.map(this::toV3Map)
			.toList();
	}

	@GetMapping("/api/v3/devicestatus/{identifier}")
	ResponseEntity<Map<String, Object>> getOne(@PathVariable String identifier) {
		AuthHelper.requirePermission("devicestatus", "read");
		try {
			UUID id = UUID.fromString(identifier);
			return this.repository.findById(id)
				.map(ds -> ResponseEntity.ok(toV3Map(ds)))
				.orElseGet(() -> ResponseEntity.notFound().build());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping("/api/v3/devicestatus")
	ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("devicestatus", "create");
		String rawJson = this.objectMapper.writeValueAsString(body);
		String device = body.get("device") instanceof String s ? s : null;
		DeviceStatus ds = new DeviceStatus(null, Instant.now(), device, jsonValueForKey(body, "uploader"),
				jsonValueForKey(body, "pump"), jsonValueForKey(body, "openaps"), jsonValueForKey(body, "loop"),
				jsonValueForKey(body, "xdripjs"), JsonValue.of(rawJson));
		DeviceStatus saved = this.repository.save(ds);
		return ResponseEntity.status(201).body(toV3Map(saved));
	}

	@PutMapping("/api/v3/devicestatus/{identifier}")
	ResponseEntity<Map<String, Object>> replace(@PathVariable String identifier,
			@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("devicestatus", "update");
		try {
			UUID id = UUID.fromString(identifier);
			if (!this.repository.existsById(id)) {
				return ResponseEntity.notFound().build();
			}
			String rawJson = this.objectMapper.writeValueAsString(body);
			String device = body.get("device") instanceof String s ? s : null;
			DeviceStatus updated = new DeviceStatus(id, Instant.now(), device, jsonValueForKey(body, "uploader"),
					jsonValueForKey(body, "pump"), jsonValueForKey(body, "openaps"), jsonValueForKey(body, "loop"),
					jsonValueForKey(body, "xdripjs"), JsonValue.of(rawJson));
			return ResponseEntity.ok(toV3Map(this.repository.save(updated)));
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@DeleteMapping("/api/v3/devicestatus/{identifier}")
	ResponseEntity<Void> delete(@PathVariable String identifier) {
		AuthHelper.requirePermission("devicestatus", "delete");
		try {
			UUID id = UUID.fromString(identifier);
			if (this.repository.existsById(id)) {
				this.repository.deleteById(id);
				return ResponseEntity.noContent().build();
			}
			return ResponseEntity.notFound().build();
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	private JsonValue jsonValueForKey(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? JsonValue.of(this.objectMapper.writeValueAsString(value)) : null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> toV3Map(DeviceStatus ds) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (ds.raw() != null) {
			map.putAll(this.objectMapper.readValue(ds.raw().value(), Map.class));
		}
		map.put("identifier", ds.id().toString());
		if (ds.device() != null) {
			map.put("device", ds.device());
		}
		map.put("date", ds.createdAt().toEpochMilli());
		map.put("created_at", ds.createdAt().toString());
		map.put("srvModified", ds.createdAt().toEpochMilli());
		map.put("isValid", true);
		return map;
	}

}
