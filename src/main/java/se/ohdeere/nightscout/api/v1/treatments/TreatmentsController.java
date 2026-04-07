package se.ohdeere.nightscout.api.v1.treatments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.treatments.Treatment;
import se.ohdeere.nightscout.storage.treatments.TreatmentRepository;

import org.springframework.dao.DuplicateKeyException;
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
class TreatmentsController {

	private final TreatmentRepository treatmentRepository;

	TreatmentsController(TreatmentRepository treatmentRepository) {
		this.treatmentRepository = treatmentRepository;
	}

	@GetMapping({ "/api/v1/treatments", "/api/v1/treatments.json" })
	List<TreatmentDto> getTreatments(@RequestParam(defaultValue = "10") int count,
			@RequestParam(name = "find[eventType]", required = false) String eventType,
			@RequestParam(name = "find[created_at][$gte]", required = false) String createdAtGte) {
		AuthHelper.requirePermission("treatments", "read");
		if (eventType != null && createdAtGte != null) {
			return this.treatmentRepository.findByEventTypeSince(eventType, Instant.parse(createdAtGte))
				.stream()
				.map(TreatmentDto::from)
				.toList();
		}
		if (createdAtGte != null) {
			return this.treatmentRepository.findSince(Instant.parse(createdAtGte))
				.stream()
				.map(TreatmentDto::from)
				.toList();
		}
		return this.treatmentRepository.findLatest(Math.min(count, 1000)).stream().map(TreatmentDto::from).toList();
	}

	@PostMapping({ "/api/v1/treatments", "/api/v1/treatments.json" })
	ResponseEntity<List<TreatmentDto>> postTreatments(@RequestBody List<TreatmentDto> dtos) {
		AuthHelper.requirePermission("treatments", "create");
		List<TreatmentDto> saved = dtos.stream().map(dto -> {
			try {
				return TreatmentDto.from(this.treatmentRepository.save(dto.toTreatment()));
			}
			catch (DuplicateKeyException ex) {
				return null;
			}
		}).filter(t -> t != null).toList();
		return ResponseEntity.ok(saved);
	}

	/**
	 * Update an existing treatment by {@code _id}. The DTO must contain {@code _id}; all
	 * other fields are merged onto the persisted record (only non-null fields overwrite).
	 */
	@PutMapping({ "/api/v1/treatments", "/api/v1/treatments.json" })
	ResponseEntity<TreatmentDto> putTreatment(@RequestBody TreatmentDto dto) {
		AuthHelper.requirePermission("treatments", "update");
		if (dto.id() == null) {
			return ResponseEntity.badRequest().build();
		}
		UUID id;
		try {
			id = UUID.fromString(dto.id());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().build();
		}
		return this.treatmentRepository.findById(id).map(existing -> {
			Treatment merged = new Treatment(existing.id(),
					dto.eventType() != null ? dto.eventType() : existing.eventType(),
					dto.createdAtStr() != null ? dto.createdAtStr() : existing.createdAtStr(),
					dto.createdAtStr() != null ? Instant.parse(dto.createdAtStr()) : existing.createdAt(),
					dto.enteredBy() != null ? dto.enteredBy() : existing.enteredBy(),
					dto.notes() != null ? dto.notes() : existing.notes(),
					dto.insulin() != null ? dto.insulin() : existing.insulin(),
					dto.carbs() != null ? dto.carbs() : existing.carbs(),
					dto.glucose() != null ? dto.glucose() : existing.glucose(),
					dto.glucoseType() != null ? dto.glucoseType() : existing.glucoseType(),
					dto.duration() != null ? dto.duration() : existing.duration(), existing.utcOffset(),
					dto.syncIdentifier() != null ? dto.syncIdentifier() : existing.syncIdentifier(),
					dto.insulinType() != null ? dto.insulinType() : existing.insulinType(),
					existing.details() != null ? existing.details() : JsonValue.empty());
			return ResponseEntity.ok(TreatmentDto.from(this.treatmentRepository.save(merged)));
		}).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/api/v1/treatments/{id}")
	ResponseEntity<Void> deleteTreatment(@PathVariable UUID id) {
		AuthHelper.requirePermission("treatments", "delete");
		this.treatmentRepository.deleteById(id);
		return ResponseEntity.ok().build();
	}

}
