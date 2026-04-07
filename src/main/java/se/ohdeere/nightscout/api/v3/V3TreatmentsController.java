package se.ohdeere.nightscout.api.v3;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.treatments.Treatment;
import se.ohdeere.nightscout.storage.treatments.TreatmentRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class V3TreatmentsController {

	private final TreatmentRepository treatmentRepository;

	V3TreatmentsController(TreatmentRepository treatmentRepository) {
		this.treatmentRepository = treatmentRepository;
	}

	@GetMapping("/api/v3/treatments")
	List<Map<String, Object>> list(@RequestParam(defaultValue = "100") int limit,
			@RequestParam(name = "date$gte", required = false) Long dateGte,
			@RequestParam(name = "eventType$eq", required = false) String eventType) {
		AuthHelper.requirePermission("treatments", "read");
		List<Treatment> all;
		if (dateGte != null) {
			all = this.treatmentRepository.findSince(Instant.ofEpochMilli(dateGte));
		}
		else if (eventType != null) {
			Treatment latest = this.treatmentRepository.findLatestByEventType(eventType);
			all = latest != null ? List.of(latest) : List.of();
		}
		else {
			all = this.treatmentRepository.findLatest(Math.min(limit, 1000));
		}
		return all.stream().map(this::toV3Map).limit(Math.min(limit, 1000)).toList();
	}

	@GetMapping("/api/v3/treatments/history/{lastModified}")
	List<Map<String, Object>> history(@PathVariable long lastModified) {
		AuthHelper.requirePermission("treatments", "read");
		return this.treatmentRepository.findSince(Instant.ofEpochMilli(lastModified))
			.stream()
			.map(this::toV3Map)
			.toList();
	}

	@GetMapping("/api/v3/treatments/{identifier}")
	ResponseEntity<Map<String, Object>> getOne(@PathVariable String identifier) {
		AuthHelper.requirePermission("treatments", "read");
		try {
			UUID id = UUID.fromString(identifier);
			return this.treatmentRepository.findById(id)
				.map(t -> ResponseEntity.ok(toV3Map(t)))
				.orElseGet(() -> ResponseEntity.notFound().build());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping("/api/v3/treatments")
	ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("treatments", "create");
		Treatment treatment = fromBody(body, null);
		try {
			Treatment saved = this.treatmentRepository.save(treatment);
			return ResponseEntity.status(201).body(toV3Map(saved));
		}
		catch (DuplicateKeyException ex) {
			Treatment existing = this.treatmentRepository.findLatestByEventType(treatment.eventType());
			return ResponseEntity
				.ok(Map.of("identifier", existing != null ? existing.id().toString() : "", "isDeduplication", true));
		}
	}

	@PutMapping("/api/v3/treatments/{identifier}")
	ResponseEntity<Map<String, Object>> replace(@PathVariable String identifier,
			@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("treatments", "update");
		try {
			UUID id = UUID.fromString(identifier);
			return this.treatmentRepository.findById(id).map(existing -> {
				Treatment updated = fromBody(body, existing.id());
				return ResponseEntity.ok(toV3Map(this.treatmentRepository.save(updated)));
			}).orElseGet(() -> ResponseEntity.notFound().build());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@PatchMapping("/api/v3/treatments/{identifier}")
	ResponseEntity<Map<String, Object>> patch(@PathVariable String identifier, @RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("treatments", "update");
		try {
			UUID id = UUID.fromString(identifier);
			return this.treatmentRepository.findById(id).map(existing -> {
				Treatment merged = mergeBody(existing, body);
				return ResponseEntity.ok(toV3Map(this.treatmentRepository.save(merged)));
			}).orElseGet(() -> ResponseEntity.notFound().build());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@DeleteMapping("/api/v3/treatments/{identifier}")
	ResponseEntity<Void> delete(@PathVariable String identifier) {
		AuthHelper.requirePermission("treatments", "delete");
		try {
			UUID id = UUID.fromString(identifier);
			if (this.treatmentRepository.existsById(id)) {
				this.treatmentRepository.deleteById(id);
				return ResponseEntity.noContent().build();
			}
			return ResponseEntity.notFound().build();
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	private Treatment fromBody(Map<String, Object> body, UUID existingId) {
		String eventType = stringOf(body.get("eventType"));
		String createdAtStr = stringOf(body.get("created_at"));
		Instant createdAt = createdAtStr != null ? Instant.parse(createdAtStr) : Instant.now();
		return new Treatment(existingId, eventType != null ? eventType : "Note",
				createdAtStr != null ? createdAtStr : createdAt.toString(), createdAt, stringOf(body.get("enteredBy")),
				stringOf(body.get("notes")), doubleOf(body.get("insulin")), doubleOf(body.get("carbs")),
				doubleOf(body.get("glucose")), stringOf(body.get("glucoseType")), doubleOf(body.get("duration")), 0,
				stringOf(body.get("syncIdentifier")), stringOf(body.get("insulinType")), JsonValue.empty());
	}

	private Treatment mergeBody(Treatment existing, Map<String, Object> body) {
		String eventType = body.containsKey("eventType") ? stringOf(body.get("eventType")) : existing.eventType();
		String createdAtStr = body.containsKey("created_at") ? stringOf(body.get("created_at"))
				: existing.createdAtStr();
		Instant createdAt = createdAtStr != null ? Instant.parse(createdAtStr) : existing.createdAt();
		return new Treatment(existing.id(), eventType, createdAtStr, createdAt,
				body.containsKey("enteredBy") ? stringOf(body.get("enteredBy")) : existing.enteredBy(),
				body.containsKey("notes") ? stringOf(body.get("notes")) : existing.notes(),
				body.containsKey("insulin") ? doubleOf(body.get("insulin")) : existing.insulin(),
				body.containsKey("carbs") ? doubleOf(body.get("carbs")) : existing.carbs(),
				body.containsKey("glucose") ? doubleOf(body.get("glucose")) : existing.glucose(),
				body.containsKey("glucoseType") ? stringOf(body.get("glucoseType")) : existing.glucoseType(),
				body.containsKey("duration") ? doubleOf(body.get("duration")) : existing.duration(),
				existing.utcOffset(),
				body.containsKey("syncIdentifier") ? stringOf(body.get("syncIdentifier")) : existing.syncIdentifier(),
				body.containsKey("insulinType") ? stringOf(body.get("insulinType")) : existing.insulinType(),
				existing.details() != null ? existing.details() : JsonValue.empty());
	}

	private Map<String, Object> toV3Map(Treatment t) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("identifier", t.id().toString());
		map.put("eventType", t.eventType());
		map.put("date", t.createdAt().toEpochMilli());
		map.put("created_at", t.createdAtStr());
		if (t.enteredBy() != null) {
			map.put("enteredBy", t.enteredBy());
		}
		if (t.notes() != null) {
			map.put("notes", t.notes());
		}
		if (t.insulin() != null) {
			map.put("insulin", t.insulin());
		}
		if (t.carbs() != null) {
			map.put("carbs", t.carbs());
		}
		if (t.glucose() != null) {
			map.put("glucose", t.glucose());
		}
		if (t.glucoseType() != null) {
			map.put("glucoseType", t.glucoseType());
		}
		if (t.duration() != null) {
			map.put("duration", t.duration());
		}
		if (t.syncIdentifier() != null) {
			map.put("syncIdentifier", t.syncIdentifier());
		}
		if (t.insulinType() != null) {
			map.put("insulinType", t.insulinType());
		}
		map.put("srvModified", t.createdAt().toEpochMilli());
		map.put("isValid", true);
		return map;
	}

	private static String stringOf(Object o) {
		return o != null ? o.toString() : null;
	}

	private static Double doubleOf(Object o) {
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
