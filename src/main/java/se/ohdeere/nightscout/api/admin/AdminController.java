package se.ohdeere.nightscout.api.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.service.admin.AdminService;
import se.ohdeere.nightscout.service.admin.AdminService.CreatedKey;
import se.ohdeere.nightscout.service.auth.NightscoutAuth;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.admin.ApiKey;
import se.ohdeere.nightscout.storage.admin.AuditEntry;
import se.ohdeere.nightscout.storage.admin.RuntimeSetting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/admin")
class AdminController {

	private final AdminService admin;

	private final ObjectMapper objectMapper;

	AdminController(AdminService admin, ObjectMapper objectMapper) {
		this.admin = admin;
		this.objectMapper = objectMapper;
	}

	/* ---------- API keys ---------- */

	public record CreateKeyRequest(String name, String scope, Instant expiresAt) {
	}

	@GetMapping("/keys")
	List<Map<String, Object>> listKeys() {
		return this.admin.list().stream().map(AdminController::toView).toList();
	}

	@PostMapping("/keys")
	ResponseEntity<Map<String, Object>> createKey(@RequestBody CreateKeyRequest req) {
		NightscoutAuth current = AuthHelper.current();
		CreatedKey created = this.admin.create(req.name(), req.scope(), current.subject(), req.expiresAt());
		Map<String, Object> body = new java.util.LinkedHashMap<>(toView(created.key()));
		body.put("token", created.plaintext()); // returned exactly once
		return ResponseEntity.ok(body);
	}

	@DeleteMapping("/keys/{id}")
	ResponseEntity<Void> revokeKey(@PathVariable UUID id) {
		this.admin.revoke(id, AuthHelper.current().subject());
		return ResponseEntity.noContent().build();
	}

	/* ---------- audit ---------- */

	@GetMapping("/audit")
	List<AuditEntry> audit(@RequestParam(defaultValue = "100") int limit) {
		return this.admin.recentAudit(limit);
	}

	/* ---------- runtime settings ---------- */

	@GetMapping("/settings")
	List<Map<String, Object>> listSettings() {
		return this.admin.listSettings().stream().map(AdminController::settingView).toList();
	}

	@GetMapping("/settings/{key}")
	ResponseEntity<Map<String, Object>> getSetting(@PathVariable String key) {
		return this.admin.getSetting(key)
			.<ResponseEntity<Map<String, Object>>>map(s -> ResponseEntity.ok(settingView(s)))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PutMapping("/settings/{key}")
	ResponseEntity<Map<String, Object>> putSetting(@PathVariable String key, @RequestBody Object value) {
		String json = this.objectMapper.writeValueAsString(value);
		RuntimeSetting saved = this.admin.putSetting(key, JsonValue.of(json), AuthHelper.current().subject());
		return ResponseEntity.ok(settingView(saved));
	}

	@DeleteMapping("/settings/{key}")
	ResponseEntity<Void> deleteSetting(@PathVariable String key) {
		this.admin.deleteSetting(key, AuthHelper.current().subject());
		return ResponseEntity.noContent().build();
	}

	private static Map<String, Object> settingView(RuntimeSetting s) {
		Map<String, Object> m = new java.util.LinkedHashMap<>();
		m.put("key", s.key());
		m.put("value", s.value() != null ? s.value().value() : null);
		m.put("updatedAt", s.updatedAt());
		m.put("updatedBy", s.updatedBy());
		return m;
	}

	private static Map<String, Object> toView(ApiKey k) {
		Map<String, Object> m = new java.util.LinkedHashMap<>();
		m.put("id", k.id());
		m.put("name", k.name());
		m.put("scope", k.scope());
		m.put("createdAt", k.createdAt());
		m.put("createdBy", k.createdBy());
		m.put("lastUsedAt", k.lastUsedAt());
		m.put("expiresAt", k.expiresAt());
		m.put("enabled", k.enabled());
		return m;
	}

}
