package se.ohdeere.nightscout.api.v3;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.service.entries.EntryService;
import se.ohdeere.nightscout.storage.entries.Entry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3")
class ApiV3Controller {

	private final EntryService entryService;

	ApiV3Controller(EntryService entryService) {
		this.entryService = entryService;
	}

	@GetMapping("/version")
	Map<String, String> version() {
		return Map.of("version", "3.0.0", "apiVersion", "3.0.0");
	}

	@GetMapping("/status")
	Map<String, Object> status() {
		return Map.of("status", "ok", "version", "15.0.6-java", "apiVersion", "3.0.0");
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

	@GetMapping("/lastModified")
	Map<String, Object> lastModified() {
		AuthHelper.requirePermission("entries", "read");
		Entry latest = this.entryService.findCurrent().orElse(null);
		long entriesTs = (latest != null) ? latest.dateMs() : 0;
		return Map.of("collections", Map.of("entries", entriesTs), "srvDate", Instant.now().toEpochMilli());
	}

	private Map<String, Object> toV3Map(Entry e) {
		return Map.of("identifier", e.id().toString(), "type", e.type(), "date", e.dateMs(), "dateString",
				(e.dateString() != null) ? e.dateString() : "", "sgv", (e.sgv() != null) ? e.sgv() : 0, "direction",
				(e.direction() != null) ? e.direction() : "", "device", (e.device() != null) ? e.device() : "",
				"srvModified", e.createdAt().toEpochMilli(), "isValid", true);
	}

}
