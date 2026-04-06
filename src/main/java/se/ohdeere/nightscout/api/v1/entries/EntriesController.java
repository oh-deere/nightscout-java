package se.ohdeere.nightscout.api.v1.entries;

import java.util.List;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.service.entries.EntryService;
import se.ohdeere.nightscout.storage.entries.Entry;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class EntriesController {

	private final EntryService entryService;

	private final ObjectMapper objectMapper;

	EntriesController(EntryService entryService, ObjectMapper objectMapper) {
		this.entryService = entryService;
		this.objectMapper = objectMapper;
	}

	@GetMapping({ "/api/v1/entries", "/api/v1/entries.json" })
	List<EntryDto> getEntries(@RequestParam(defaultValue = "10") int count,
			@RequestParam(name = "find[type]", required = false) String type,
			@RequestParam(name = "find[date][$gte]", required = false) Long dateGte,
			@RequestParam(name = "find[date][$lte]", required = false) Long dateLte) {
		AuthHelper.requirePermission("entries", "read");
		if (type != null && dateGte != null) {
			return this.entryService.findByTypeAndDateFrom(type, dateGte).stream().map(EntryDto::from).toList();
		}
		if (dateGte != null) {
			long to = (dateLte != null) ? dateLte : Long.MAX_VALUE;
			return this.entryService.findByDateRange(dateGte, to).stream().map(EntryDto::from).toList();
		}
		if (type != null) {
			return this.entryService.findLatestByType(type, count).stream().map(EntryDto::from).toList();
		}
		return this.entryService.findLatest(count).stream().map(EntryDto::from).toList();
	}

	@GetMapping({ "/api/v1/entries/current", "/api/v1/entries/current.json" })
	ResponseEntity<List<EntryDto>> getCurrent() {
		AuthHelper.requirePermission("entries", "read");
		return this.entryService.findCurrent()
			.map(entry -> ResponseEntity.ok(List.of(EntryDto.from(entry))))
			.orElse(ResponseEntity.ok(List.of()));
	}

	@GetMapping({ "/api/v1/entries/sgv", "/api/v1/entries/sgv.json" })
	List<EntryDto> getSgv(@RequestParam(defaultValue = "10") int count) {
		AuthHelper.requirePermission("entries", "read");
		return this.entryService.findLatestByType("sgv", count).stream().map(EntryDto::from).toList();
	}

	@PostMapping({ "/api/v1/entries", "/api/v1/entries.json" })
	ResponseEntity<List<EntryDto>> postEntries(@RequestBody JsonNode body) {
		AuthHelper.requirePermission("entries", "create");
		List<EntryDto> dtos;
		if (body.isArray()) {
			dtos = List.of(this.objectMapper.treeToValue(body, EntryDto[].class));
		}
		else {
			dtos = List.of(this.objectMapper.treeToValue(body, EntryDto.class));
		}
		List<Entry> entries = dtos.stream().map(EntryDto::toEntry).toList();
		List<Entry> saved = this.entryService.saveAll(entries);
		return ResponseEntity.status(HttpStatus.OK).body(saved.stream().map(EntryDto::from).toList());
	}

	@DeleteMapping("/api/v1/entries/{id}")
	ResponseEntity<Void> deleteEntry(@PathVariable UUID id) {
		AuthHelper.requirePermission("entries", "delete");
		this.entryService.deleteById(id);
		return ResponseEntity.ok().build();
	}

}
