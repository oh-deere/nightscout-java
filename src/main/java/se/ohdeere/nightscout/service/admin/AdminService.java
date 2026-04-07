package se.ohdeere.nightscout.service.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import se.ohdeere.nightscout.service.auth.NightscoutAuth;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.admin.ApiKey;
import se.ohdeere.nightscout.storage.admin.ApiKeyRepository;
import se.ohdeere.nightscout.storage.admin.AuditEntry;
import se.ohdeere.nightscout.storage.admin.AuditEntryRepository;
import se.ohdeere.nightscout.storage.admin.RuntimeSetting;
import se.ohdeere.nightscout.storage.admin.RuntimeSettingRepository;

import org.springframework.stereotype.Service;

/**
 * Manages per-app API keys, audit log writes, and the resolution of plaintext tokens into
 * authenticated subjects. Bootstrap admin (env-var API_SECRET) is handled separately by
 * AuthServiceImpl.
 */
@Service
public class AdminService {

	private static final SecureRandom RNG = new SecureRandom();

	private final ApiKeyRepository keys;

	private final AuditEntryRepository audit;

	private final RuntimeSettingRepository settings;

	private final ApiKeyTouchService touch;

	public AdminService(ApiKeyRepository keys, AuditEntryRepository audit, RuntimeSettingRepository settings,
			ApiKeyTouchService touch) {
		this.keys = keys;
		this.audit = audit;
		this.settings = settings;
		this.touch = touch;
	}

	/* ---------- runtime settings ---------- */

	public List<RuntimeSetting> listSettings() {
		return this.settings.findAllOrdered();
	}

	public Optional<RuntimeSetting> getSetting(String key) {
		return this.settings.findById(key);
	}

	public RuntimeSetting putSetting(String key, JsonValue value, String actor) {
		JsonValue before = this.settings.findById(key).map(RuntimeSetting::value).orElse(null);
		Instant now = Instant.now();
		this.settings.upsert(key, value, now, actor);
		recordAudit(actor, actorKind(actor), "settings.update", key, before, value);
		return new RuntimeSetting(key, value, now, actor);
	}

	public void deleteSetting(String key, String actor) {
		this.settings.findById(key).ifPresent(existing -> {
			this.settings.deleteById(key);
			recordAudit(actor, actorKind(actor), "settings.delete", key, existing.value(), null);
		});
	}

	/* ---------- key lifecycle ---------- */

	public record CreatedKey(ApiKey key, String plaintext) {
	}

	public CreatedKey create(String name, String scope, String createdBy, Instant expiresAt) {
		validateScope(scope);
		String plaintext = randomToken();
		String hash = sha256(plaintext);
		ApiKey row = new ApiKey(null, name, hash, scope, Instant.now(), createdBy, null, expiresAt, true);
		ApiKey saved = this.keys.save(row);
		recordAudit(createdBy, actorKind(createdBy), "key.create", name, null,
				JsonValue.of("{\"scope\":\"" + scope + "\"}"));
		return new CreatedKey(saved, plaintext);
	}

	public List<ApiKey> list() {
		return this.keys.findAllOrdered();
	}

	public void revoke(UUID id, String actor) {
		this.keys.findById(id).ifPresent(k -> {
			ApiKey disabled = new ApiKey(k.id(), k.name(), k.tokenHash(), k.scope(), k.createdAt(), k.createdBy(),
					k.lastUsedAt(), k.expiresAt(), false);
			this.keys.save(disabled);
			recordAudit(actor, actorKind(actor), "key.revoke", k.name(), JsonValue.of("{\"enabled\":true}"),
					JsonValue.of("{\"enabled\":false}"));
		});
	}

	/* ---------- token resolution (called from AuthServiceImpl) ---------- */

	public Optional<NightscoutAuth> resolveToken(String plaintext) {
		if (plaintext == null || plaintext.isBlank()) {
			return Optional.empty();
		}
		String hash = sha256(plaintext);
		return this.keys.findByTokenHash(hash).flatMap(k -> {
			if (!k.enabled()) {
				return Optional.empty();
			}
			if (k.expiresAt() != null && Instant.now().isAfter(k.expiresAt())) {
				return Optional.empty();
			}
			this.touch.touchAsync(k.id());
			return Optional.of(toAuth(k));
		});
	}

	/* ---------- audit ---------- */

	public void recordAudit(String actorSubject, String actorKind, String action, String target, JsonValue before,
			JsonValue after) {
		AuditEntry entry = new AuditEntry(null, Instant.now(), actorSubject, actorKind, action, target, before, after);
		this.audit.save(entry);
	}

	public List<AuditEntry> recentAudit(int limit) {
		return this.audit.findRecent(Math.min(limit, 500));
	}

	/* ---------- helpers ---------- */

	private static NightscoutAuth toAuth(ApiKey k) {
		return switch (k.scope()) {
			case "admin" -> NightscoutAuth.adminAuth("key:" + k.name());
			case "write" ->
				new NightscoutAuth("key:" + k.name(), List.of("api:*:read", "api:*:create", "api:*:update"), false);
			case "read" -> new NightscoutAuth("key:" + k.name(), List.of("api:*:read"), false);
			default -> NightscoutAuth.ANONYMOUS;
		};
	}

	private static String actorKind(String subject) {
		if (subject == null || "api-secret".equals(subject)) {
			return "bootstrap-secret";
		}
		if (subject.startsWith("key:")) {
			return "api-key";
		}
		return "oauth";
	}

	private static void validateScope(String scope) {
		if (!"read".equals(scope) && !"write".equals(scope) && !"admin".equals(scope)) {
			throw new IllegalArgumentException("scope must be read, write, or admin");
		}
	}

	private static String randomToken() {
		byte[] buf = new byte[24];
		RNG.nextBytes(buf);
		return HexFormat.of().formatHex(buf);
	}

	static String sha256(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

}
