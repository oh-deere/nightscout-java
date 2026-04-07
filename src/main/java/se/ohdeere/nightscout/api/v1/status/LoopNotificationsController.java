package se.ohdeere.nightscout.api.v1.status;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.treatments.Treatment;
import se.ohdeere.nightscout.storage.treatments.TreatmentRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the Loop "remote command" endpoint at
 * {@code POST /api/v2/notifications/loop}.
 *
 * Loop uses this single endpoint for: starting/cancelling temporary overrides, remote
 * bolus entries, and remote carb entries. Upstream Nightscout's loop notifications plugin
 * validates the OTP, then pushes an APNs notification to the Loop app on the user's
 * phone, and Loop performs the action locally and uploads a confirmation as a treatment.
 *
 * Our implementation persists the request as a treatment so the action is auditable in
 * the standard treatment log. The APNs side-channel is intentionally not implemented —
 * for our single-user deployment we surface the request via the regular treatment stream
 * and trust Loop to read it back.
 */
@RestController
class LoopNotificationsController {

	private static final Logger LOG = LoggerFactory.getLogger(LoopNotificationsController.class);

	private final TreatmentRepository treatmentRepository;

	LoopNotificationsController(TreatmentRepository treatmentRepository) {
		this.treatmentRepository = treatmentRepository;
	}

	@PostMapping("/api/v2/notifications/loop")
	ResponseEntity<Map<String, Object>> loopNotification(@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("notifications", "create");
		String eventType = stringOf(body.get("eventType"));
		LOG.info("Loop notification received: eventType={}, body={}", eventType, body);

		Treatment treatment = buildTreatment(body, eventType);
		try {
			Treatment saved = this.treatmentRepository.save(treatment);
			return ResponseEntity.ok(Map.of("ok", true, "_id", saved.id().toString(), "eventType",
					saved.eventType() != null ? saved.eventType() : ""));
		}
		catch (DuplicateKeyException ex) {
			// Same eventType + created_at already exists; treat as idempotent success
			return ResponseEntity.ok(Map.of("ok", true, "duplicate", true));
		}
	}

	private Treatment buildTreatment(Map<String, Object> body, String eventType) {
		String createdAtStr = stringOf(body.get("created_at"));
		Instant createdAt = createdAtStr != null ? Instant.parse(createdAtStr) : Instant.now();
		String createdAtKey = createdAtStr != null ? createdAtStr : createdAt.toString();

		String notes = buildNotes(body);
		Double insulin = parseDouble(body.get("remoteBolus"));
		Double carbs = parseDouble(body.get("remoteCarbs"));
		Double duration = parseDouble(body.get("duration"));

		return new Treatment(null, eventType != null ? eventType : "Note", createdAtKey, createdAt, "loop://remote",
				notes, insulin, carbs, null, null, duration, 0, null, null, JsonValue.empty());
	}

	private String buildNotes(Map<String, Object> body) {
		StringBuilder sb = new StringBuilder();
		String reason = stringOf(body.get("reason"));
		if (reason != null) {
			sb.append("reason=").append(reason);
		}
		String reasonDisplay = stringOf(body.get("reasonDisplay"));
		if (reasonDisplay != null) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append("display=").append(reasonDisplay);
		}
		String existingNotes = stringOf(body.get("notes"));
		if (existingNotes != null && !existingNotes.isBlank()) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(existingNotes);
		}
		String absorption = stringOf(body.get("remoteAbsorption"));
		if (absorption != null) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append("absorption=").append(absorption).append('h');
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	private static String stringOf(Object o) {
		return o != null ? o.toString() : null;
	}

	private static Double parseDouble(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		if (o instanceof String s && !s.isBlank()) {
			try {
				return Double.parseDouble(s);
			}
			catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}

}
