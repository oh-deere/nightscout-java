package se.ohdeere.nightscout.api.v1.food;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.food.Activity;
import se.ohdeere.nightscout.storage.food.ActivityRepository;

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
class ActivityController {

	private final ActivityRepository activityRepository;

	ActivityController(ActivityRepository activityRepository) {
		this.activityRepository = activityRepository;
	}

	@GetMapping({ "/api/v1/activity", "/api/v1/activity.json" })
	List<ActivityDto> getActivity(@RequestParam(defaultValue = "100") int count) {
		AuthHelper.requirePermission("activity", "read");
		return this.activityRepository.findLatest(Math.min(count, 1000)).stream().map(ActivityDto::from).toList();
	}

	@PostMapping({ "/api/v1/activity", "/api/v1/activity.json" })
	ResponseEntity<ActivityDto> postActivity(@RequestBody ActivityDto dto) {
		AuthHelper.requirePermission("activity", "create");
		Activity saved = this.activityRepository.save(dto.toActivity());
		return ResponseEntity.ok(ActivityDto.from(saved));
	}

	@PutMapping({ "/api/v1/activity", "/api/v1/activity.json" })
	ResponseEntity<ActivityDto> putActivity(@RequestBody ActivityDto dto) {
		AuthHelper.requirePermission("activity", "update");
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
		return this.activityRepository.findById(id).map(existing -> {
			Activity merged = new Activity(existing.id(), existing.createdAt(),
					dto.name() != null ? dto.name() : existing.name(),
					dto.duration() != null ? dto.duration() : existing.duration(),
					existing.details() != null ? existing.details() : JsonValue.empty());
			return ResponseEntity.ok(ActivityDto.from(this.activityRepository.save(merged)));
		}).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/api/v1/activity/{id}")
	ResponseEntity<Void> deleteActivity(@PathVariable UUID id) {
		AuthHelper.requirePermission("activity", "delete");
		this.activityRepository.deleteById(id);
		return ResponseEntity.ok().build();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record ActivityDto(@JsonProperty("_id") String id, @JsonProperty("created_at") String createdAt, String name,
			Double duration) {

		static ActivityDto from(Activity a) {
			return new ActivityDto(a.id() != null ? a.id().toString() : null,
					a.createdAt() != null ? a.createdAt().toString() : null, a.name(), a.duration());
		}

		Activity toActivity() {
			return new Activity(null, Instant.now(), this.name, this.duration, JsonValue.empty());
		}

	}

}
