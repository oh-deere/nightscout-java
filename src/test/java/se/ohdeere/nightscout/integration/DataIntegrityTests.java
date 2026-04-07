package se.ohdeere.nightscout.integration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import se.ohdeere.nightscout.TestcontainersConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Data integrity guarantees independent of the wire protocol: the same payload posted
 * twice must not produce duplicate rows, and a thousand-entry batch insert must complete
 * cleanly.
 */
@SpringBootTest(properties = "nightscout.api-secret=test-secret")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class DataIntegrityTests {

	@Autowired
	MockMvc mockMvc;

	private static final String ADMIN_HEADER = sha1("test-secret");

	// Fixed historical anchor (2020-01-01T00:00:00Z). Tests share a single
	// testcontainer DB; using pre-2023 timestamps keeps these rows out of any
	// "latest entry" assertion in sibling test classes.
	private static final long EPOCH_2020 = 1577836800000L;

	@Test
	void postingSameEntryTwiceIsDeduplicated() throws Exception {
		long ts = EPOCH_2020;
		String iso = Instant.ofEpochMilli(ts).toString();
		String body = """
				[{
					"type": "sgv",
					"date": %d,
					"dateString": "%s",
					"sysTime": "%s",
					"sgv": 173,
					"direction": "Flat",
					"device": "dedup-test"
				}]
				""".formatted(ts, iso, iso);

		// First post
		this.mockMvc
			.perform(post("/api/v1/entries.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());

		// Second identical post — must not create a second row
		this.mockMvc
			.perform(post("/api/v1/entries.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());

		// Query by the exact timestamp window so we don't compete with the 1000-row
		// batch test in this same class.
		MvcResult res = this.mockMvc
			.perform(get("/api/v1/entries.json").param("find[date][$gte]", String.valueOf(ts))
				.param("find[date][$lte]", String.valueOf(ts))
				.header("api-secret", ADMIN_HEADER))
			.andExpect(status().isOk())
			.andReturn();

		String responseBody = res.getResponse().getContentAsString();
		long matches = countOccurrences(responseBody, "\"device\":\"dedup-test\"");
		assertThat(matches).as("dedup-test entry should appear exactly once").isEqualTo(1);
	}

	@Test
	void largeBatchInsertCompletes() throws Exception {
		long base = EPOCH_2020 + 1000L * 60 * 60; // 2020-01-01 01:00:00Z
		StringBuilder body = new StringBuilder("[");
		int count = 1000;
		for (int i = 0; i < count; i++) {
			long ts = base + i * 1000L;
			String iso = Instant.ofEpochMilli(ts).toString();
			if (i > 0) {
				body.append(',');
			}
			body.append("""
					{
						"type":"sgv",
						"date":%d,
						"dateString":"%s",
						"sysTime":"%s",
						"sgv":%d,
						"direction":"Flat",
						"device":"batch-test"
					}""".formatted(ts, iso, iso, 100 + (i % 50)));
		}
		body.append(']');

		this.mockMvc
			.perform(post("/api/v1/entries.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(count));
	}

	@Test
	void readOnlyApiKeyCannotPost() throws Exception {
		// Create a read-only key (admin endpoint)
		MvcResult created = this.mockMvc
			.perform(post("/api/v2/admin/keys").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"readonly-int-test","scope":"read"}"""))
			.andExpect(status().isOk())
			.andReturn();

		String responseBody = created.getResponse().getContentAsString();
		String token = extractField(responseBody, "token");

		// GET with the read-only token works
		this.mockMvc.perform(get("/api/v1/entries.json").param("token", token)).andExpect(status().isOk());

		// POST with the same token must be forbidden
		long ts = EPOCH_2020 + 5000L; // distinct historical timestamp
		String iso = Instant.ofEpochMilli(ts).toString();
		String entry = """
				[{"type":"sgv","date":%d,"dateString":"%s","sysTime":"%s","sgv":200,"direction":"Flat","device":"ro-test"}]
				"""
			.formatted(ts, iso, iso);
		this.mockMvc
			.perform(post("/api/v1/entries.json").param("token", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(entry))
			.andExpect(status().isForbidden());
	}

	/* ---------- helpers ---------- */

	private static long countOccurrences(String haystack, String needle) {
		long count = 0;
		int idx = 0;
		while ((idx = haystack.indexOf(needle, idx)) != -1) {
			count++;
			idx += needle.length();
		}
		return count;
	}

	private static String extractField(String json, String field) {
		String marker = "\"" + field + "\":\"";
		int start = json.indexOf(marker);
		if (start < 0) {
			throw new IllegalArgumentException("field not found: " + field);
		}
		start += marker.length();
		int end = json.indexOf('"', start);
		return json.substring(start, end);
	}

	private static String sha1(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
