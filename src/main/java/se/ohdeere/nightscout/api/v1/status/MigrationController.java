package se.ohdeere.nightscout.api.v1.status;

import java.util.Map;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.service.migration.MongoMigrationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoint to trigger data migration from an existing Nightscout instance.
 */
@RestController
class MigrationController {

	private final MongoMigrationService migrationService;

	MigrationController(MongoMigrationService migrationService) {
		this.migrationService = migrationService;
	}

	@PostMapping("/api/v1/admin/migrate")
	ResponseEntity<Map<String, Object>> migrate(@RequestBody Map<String, Object> body) {
		AuthHelper.requirePermission("admin", "create");
		String sourceUrl = (String) body.get("sourceUrl");
		String apiSecretHash = (String) body.get("apiSecretHash");
		int count = (body.get("count") instanceof Number n) ? n.intValue() : 10000;

		int imported = this.migrationService.importEntries(sourceUrl, apiSecretHash, count);
		return ResponseEntity.ok(Map.of("imported", imported));
	}

}
