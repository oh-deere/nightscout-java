package se.ohdeere.nightscout.service.migration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.service.entries.EntryService;
import se.ohdeere.nightscout.storage.entries.Entry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

/**
 * Migrates data from an existing Nightscout instance (via its REST API) into the local
 * PostgreSQL database. This is simpler and safer than connecting directly to MongoDB.
 */
@Service
public class MongoMigrationService {

	private static final Logger LOG = LoggerFactory.getLogger(MongoMigrationService.class);

	private final EntryService entryService;

	private final ObjectMapper objectMapper;

	private final HttpClient httpClient;

	public MongoMigrationService(EntryService entryService, ObjectMapper objectMapper) {
		this.entryService = entryService;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newHttpClient();
	}

	/**
	 * Import entries from a running Nightscout instance.
	 * @param nightscoutUrl base URL (e.g. "https://noah.ohdeere.se")
	 * @param apiSecretHash SHA-1 hash of the API secret
	 * @param count max entries to fetch
	 * @return number of entries imported
	 */
	public int importEntries(String nightscoutUrl, String apiSecretHash, int count) {
		LOG.info("Migration: importing up to {} entries from {}", count, nightscoutUrl);
		AtomicInteger imported = new AtomicInteger(0);

		try {
			int batchSize = 1000;
			for (int offset = 0; offset < count; offset += batchSize) {
				int fetchCount = Math.min(batchSize, count - offset);
				String url = nightscoutUrl + "/api/v1/entries.json?count=" + fetchCount + "&find[date][$lte]="
						+ System.currentTimeMillis();

				HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("api-secret", apiSecretHash)
					.GET()
					.build();

				HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

				if (response.statusCode() != 200) {
					LOG.error("Migration: HTTP {} from {}", response.statusCode(), url);
					break;
				}

				JsonNode array = this.objectMapper.readTree(response.body());
				if (!array.isArray() || array.isEmpty()) {
					LOG.info("Migration: no more entries to fetch");
					break;
				}

				List<Entry> entries = new ArrayList<>();
				for (JsonNode node : array) {
					entries.add(new Entry(null, node.has("type") ? node.get("type").asText() : "sgv",
							node.has("date") ? node.get("date").asLong() : 0,
							node.has("dateString") ? node.get("dateString").asText() : null,
							node.has("sysTime") ? node.get("sysTime").asText() : null,
							node.has("sgv") ? node.get("sgv").asInt() : null,
							node.has("direction") ? node.get("direction").asText() : null,
							node.has("noise") ? node.get("noise").asInt() : null,
							node.has("filtered") ? node.get("filtered").asDouble() : null,
							node.has("unfiltered") ? node.get("unfiltered").asDouble() : null,
							node.has("rssi") ? node.get("rssi").asInt() : null,
							node.has("device") ? node.get("device").asText() : null,
							node.has("utcOffset") ? node.get("utcOffset").asInt() : 0, Instant.now()));
				}

				List<Entry> saved = this.entryService.saveAll(entries);
				imported.addAndGet(saved.size());
				LOG.info("Migration: batch imported {}/{} entries (total: {})", saved.size(), entries.size(),
						imported.get());

				if (array.size() < fetchCount) {
					break;
				}
			}
		}
		catch (Exception ex) {
			LOG.error("Migration error", ex);
		}

		LOG.info("Migration complete: {} entries imported", imported.get());
		return imported.get();
	}

}
