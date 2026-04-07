package se.ohdeere.nightscout.api.v3;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.service.entries.EntryService;
import se.ohdeere.nightscout.storage.devicestatus.DeviceStatusRepository;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.profiles.ProfileRepository;
import se.ohdeere.nightscout.storage.treatments.TreatmentRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3")
class ApiV3Controller {

	private final EntryService entryService;

	private final TreatmentRepository treatmentRepository;

	private final DeviceStatusRepository deviceStatusRepository;

	private final ProfileRepository profileRepository;

	ApiV3Controller(EntryService entryService, TreatmentRepository treatmentRepository,
			DeviceStatusRepository deviceStatusRepository, ProfileRepository profileRepository) {
		this.entryService = entryService;
		this.treatmentRepository = treatmentRepository;
		this.deviceStatusRepository = deviceStatusRepository;
		this.profileRepository = profileRepository;
	}

	@GetMapping("/version")
	Map<String, String> version() {
		return Map.of("version", "3.0.0", "apiVersion", "3.0.0");
	}

	@GetMapping("/status")
	Map<String, Object> status() {
		return Map.of("status", "ok", "version", "15.0.6-java", "apiVersion", "3.0.0", "serverTimeEpoch",
				Instant.now().toEpochMilli(), "collections",
				List.of("entries", "treatments", "profile", "devicestatus", "food", "settings"));
	}

	@GetMapping("/entries")
	List<Map<String, Object>> getEntries(@RequestParam(defaultValue = "10") int limit,
			@RequestParam(name = "sort$desc", defaultValue = "date") String sortDesc,
			@RequestParam(name = "date$gte", required = false) Long dateGte,
			@RequestParam(name = "type$eq", required = false) String type) {
		AuthHelper.requirePermission("entries", "read");

		List<Entry> entries;
		if (type != null && dateGte != null) {
			entries = this.entryService.findByTypeAndDateFrom(type, dateGte);
		}
		else if (dateGte != null) {
			entries = this.entryService.findByDateRange(dateGte, Long.MAX_VALUE);
		}
		else if (type != null) {
			entries = this.entryService.findLatestByType(type, limit);
		}
		else {
			entries = this.entryService.findLatest(limit);
		}

		return entries.stream().map(this::toV3Map).toList();
	}

	/**
	 * Lookup a single entry by identifier (UUID). Used by uploaders that POST then GET to
	 * confirm.
	 */
	@GetMapping("/entries/{identifier}")
	ResponseEntity<Map<String, Object>> getEntryByIdentifier(@PathVariable String identifier) {
		AuthHelper.requirePermission("entries", "read");
		try {
			UUID id = UUID.fromString(identifier);
			return this.entryService.findById(id)
				.map(e -> ResponseEntity.ok(toV3Map(e)))
				.orElseGet(() -> ResponseEntity.notFound().build());
		}
		catch (IllegalArgumentException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Per-collection latest modification timestamps. AAPS/xDrip+ poll this for
	 * incremental sync.
	 */
	@GetMapping("/lastModified")
	Map<String, Object> lastModified() {
		AuthHelper.requirePermission("entries", "read");

		Map<String, Object> collections = new LinkedHashMap<>();
		Entry latestEntry = this.entryService.findCurrent().orElse(null);
		collections.put("entries", latestEntry != null ? latestEntry.dateMs() : 0L);

		var latestTreatment = this.treatmentRepository.findLatest(1).stream().findFirst().orElse(null);
		collections.put("treatments", latestTreatment != null ? latestTreatment.createdAt().toEpochMilli() : 0L);

		var latestProfile = this.profileRepository.findCurrent();
		collections.put("profile", latestProfile != null ? latestProfile.createdAt().toEpochMilli() : 0L);

		var latestStatus = this.deviceStatusRepository.findCurrent();
		collections.put("devicestatus", latestStatus != null ? latestStatus.createdAt().toEpochMilli() : 0L);

		return Map.of("collections", collections, "srvDate", Instant.now().toEpochMilli());
	}

	private Map<String, Object> toV3Map(Entry e) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("identifier", e.id().toString());
		map.put("type", e.type());
		map.put("date", e.dateMs());
		map.put("dateString", e.dateString() != null ? e.dateString() : "");
		map.put("sgv", e.sgv() != null ? e.sgv() : 0);
		map.put("direction", e.direction() != null ? e.direction() : "");
		map.put("device", e.device() != null ? e.device() : "");
		map.put("srvModified", e.createdAt().toEpochMilli());
		map.put("isValid", true);
		return map;
	}

}
