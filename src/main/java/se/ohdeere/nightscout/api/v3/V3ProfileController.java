package se.ohdeere.nightscout.api.v3;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.profiles.Profile;
import se.ohdeere.nightscout.storage.profiles.ProfileRepository;
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
class V3ProfileController {

	private final ProfileRepository repository;

	private final ObjectMapper objectMapper;

	V3ProfileController(ProfileRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/api/v3/profile")
	List<Map<String, Object>> list(@RequestParam(defaultValue = "100") int limit) {
		AuthHelper.requirePermission("profile", "read");
		return this.repository.findAllOrdered().stream().limit(limit).map(this::toV3Map).toList();
	}

	@GetMapping("/api/v3/profile/history/{lastModified}")
	List<Map<String, Object>> history(@PathVariable long lastModified) {
		AuthHelper.requirePermission("profile", "read");
		return this.repository.findAllOrdered()
			.stream()
			.filter(p -> p.createdAt().toEpochMilli() > lastModified)
			.map(this::toV3Map)
			.toList();
	}

	@GetMapping("/api/v3/profile/{identifier}")
	ResponseEntity<Map<String, Object>> getOne(@PathVariable String identifier) {
		AuthHelper.requirePermission("profile", "read");
		try {
			UUID id = UUID.fromString(identifier);
			return this.repository.findById(id)
				.map(p -> ResponseEntity.ok(toV3Map(p)))
				.orElseGet(() -> ResponseEntity.notFound().build());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping("/api/v3/profile")
	ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("profile", "create");
		String defaultProfile = body.get("defaultProfile") instanceof String s ? s : null;
		String storeJson = this.objectMapper.writeValueAsString(body.get("store"));
		Profile p = new Profile(null, Instant.now(), defaultProfile, JsonValue.of(storeJson));
		return ResponseEntity.status(201).body(toV3Map(this.repository.save(p)));
	}

	@PutMapping("/api/v3/profile/{identifier}")
	ResponseEntity<Map<String, Object>> replace(@PathVariable String identifier,
			@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("profile", "update");
		try {
			UUID id = UUID.fromString(identifier);
			return this.repository.findById(id).map(existing -> {
				String defaultProfile = body.get("defaultProfile") instanceof String s ? s : existing.defaultProfile();
				JsonValue store = body.containsKey("store")
						? JsonValue.of(this.objectMapper.writeValueAsString(body.get("store"))) : existing.store();
				Profile updated = new Profile(existing.id(), existing.createdAt(), defaultProfile, store);
				return ResponseEntity.ok(toV3Map(this.repository.save(updated)));
			}).orElseGet(() -> ResponseEntity.notFound().build());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@DeleteMapping("/api/v3/profile/{identifier}")
	ResponseEntity<Void> delete(@PathVariable String identifier) {
		AuthHelper.requirePermission("profile", "delete");
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

	@SuppressWarnings("unchecked")
	private Map<String, Object> toV3Map(Profile p) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("identifier", p.id().toString());
		map.put("defaultProfile", p.defaultProfile() != null ? p.defaultProfile() : "");
		Object store = (p.store() != null) ? this.objectMapper.readValue(p.store().value(), Map.class) : Map.of();
		map.put("store", store);
		map.put("date", p.createdAt().toEpochMilli());
		map.put("created_at", p.createdAt().toString());
		map.put("srvModified", p.createdAt().toEpochMilli());
		map.put("isValid", true);
		return map;
	}

}
