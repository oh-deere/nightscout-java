package se.ohdeere.nightscout.api.v1.devicestatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.devicestatus.DeviceStatus;
import se.ohdeere.nightscout.storage.devicestatus.DeviceStatusRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DeviceStatusController {

	private final DeviceStatusRepository deviceStatusRepository;

	private final tools.jackson.databind.ObjectMapper objectMapper;

	DeviceStatusController(DeviceStatusRepository deviceStatusRepository,
			tools.jackson.databind.ObjectMapper objectMapper) {
		this.deviceStatusRepository = deviceStatusRepository;
		this.objectMapper = objectMapper;
	}

	@GetMapping({ "/api/v1/devicestatus", "/api/v1/devicestatus.json" })
	List<Map<String, Object>> getDeviceStatus(@RequestParam(defaultValue = "10") int count) {
		AuthHelper.requirePermission("devicestatus", "read");
		return this.deviceStatusRepository.findLatest(Math.min(count, 1000)).stream().map(this::toMap).toList();
	}

	@PostMapping({ "/api/v1/devicestatus", "/api/v1/devicestatus.json" })
	ResponseEntity<String> postDeviceStatus(@RequestBody List<Map<String, Object>> statusList) {
		AuthHelper.requirePermission("devicestatus", "create");
		for (Map<String, Object> status : statusList) {
			String rawJson = this.objectMapper.writeValueAsString(status);
			String device = (String) status.get("device");
			DeviceStatus ds = new DeviceStatus(null, Instant.now(), device, jsonValueForKey(status, "uploader"),
					jsonValueForKey(status, "pump"), jsonValueForKey(status, "openaps"),
					jsonValueForKey(status, "loop"), jsonValueForKey(status, "xdripjs"), JsonValue.of(rawJson));
			this.deviceStatusRepository.save(ds);
		}
		return ResponseEntity.ok("{}");
	}

	@DeleteMapping("/api/v1/devicestatus/{id}")
	ResponseEntity<Void> deleteDeviceStatus(@PathVariable UUID id) {
		AuthHelper.requirePermission("devicestatus", "delete");
		this.deviceStatusRepository.deleteById(id);
		return ResponseEntity.ok().build();
	}

	private JsonValue jsonValueForKey(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return (value != null) ? JsonValue.of(this.objectMapper.writeValueAsString(value)) : null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> toMap(DeviceStatus ds) {
		if (ds.raw() != null) {
			Map<String, Object> map = new java.util.HashMap<>(this.objectMapper.readValue(ds.raw().value(), Map.class));
			map.put("_id", ds.id().toString());
			return map;
		}
		return Map.of("_id", ds.id().toString(), "device", ds.device(), "created_at", ds.createdAt().toString());
	}

}
