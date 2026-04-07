package se.ohdeere.nightscout.api.v1.profiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.profiles.Profile;
import se.ohdeere.nightscout.storage.profiles.ProfileRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProfilesController {

	private final ProfileRepository profileRepository;

	private final tools.jackson.databind.ObjectMapper objectMapper;

	ProfilesController(ProfileRepository profileRepository, tools.jackson.databind.ObjectMapper objectMapper) {
		this.profileRepository = profileRepository;
		this.objectMapper = objectMapper;
	}

	@GetMapping({ "/api/v1/profile", "/api/v1/profile.json", "/api/v1/profiles", "/api/v1/profiles.json" })
	List<Map<String, Object>> getProfiles() {
		AuthHelper.requirePermission("profile", "read");
		return this.profileRepository.findAllOrdered().stream().map(this::toMap).toList();
	}

	/**
	 * Returns the most recent profile as a single object (not an array). Used by AAPS.
	 */
	@GetMapping({ "/api/v1/profile/current", "/api/v1/profile/current.json" })
	ResponseEntity<Map<String, Object>> getCurrentProfile() {
		AuthHelper.requirePermission("profile", "read");
		Profile current = this.profileRepository.findCurrent();
		if (current == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(toMap(current));
	}

	@PostMapping({ "/api/v1/profile", "/api/v1/profile.json" })
	ResponseEntity<Map<String, Object>> postProfile(@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("profile", "create");
		String defaultProfile = (String) body.get("defaultProfile");
		String storeJson = this.objectMapper.writeValueAsString(body.get("store"));
		Profile profile = new Profile(null, Instant.now(), defaultProfile, JsonValue.of(storeJson));
		Profile saved = this.profileRepository.save(profile);
		return ResponseEntity.ok(toMap(saved));
	}

	/**
	 * Update an existing profile by {@code _id}. The body's {@code store} replaces the
	 * entire profile store; {@code defaultProfile} is updated if present.
	 */
	@PutMapping({ "/api/v1/profile", "/api/v1/profile.json" })
	ResponseEntity<Map<String, Object>> putProfile(@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("profile", "update");
		Object idObj = body.get("_id");
		if (idObj == null) {
			return ResponseEntity.badRequest().build();
		}
		UUID id;
		try {
			id = UUID.fromString(idObj.toString());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().build();
		}
		return this.profileRepository.findById(id).map(existing -> {
			String defaultProfile = body.containsKey("defaultProfile") ? (String) body.get("defaultProfile")
					: existing.defaultProfile();
			JsonValue store;
			if (body.containsKey("store")) {
				String storeJson = this.objectMapper.writeValueAsString(body.get("store"));
				store = JsonValue.of(storeJson);
			}
			else {
				store = existing.store();
			}
			Profile merged = new Profile(existing.id(), existing.createdAt(), defaultProfile, store);
			return ResponseEntity.ok(toMap(this.profileRepository.save(merged)));
		}).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/api/v1/profile/{id}")
	ResponseEntity<Void> deleteProfile(@PathVariable UUID id) {
		AuthHelper.requirePermission("profile", "delete");
		this.profileRepository.deleteById(id);
		return ResponseEntity.ok().build();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> toMap(Profile p) {
		Object store = (p.store() != null) ? this.objectMapper.readValue(p.store().value(), Map.class) : Map.of();
		return Map.of("_id", p.id().toString(), "defaultProfile",
				(p.defaultProfile() != null) ? p.defaultProfile() : "", "store", store, "created_at",
				p.createdAt().toString());
	}

}
