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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that POST captured uploader payloads (xDrip+, AAPS, Loop) and verify
 * round-trip preservation of the fields each uploader cares about.
 *
 * <p>
 * Payload templates live under {@code src/test/resources/payloads/}. Placeholder tokens
 * ({@code __NOW__}, {@code __NOW_ISO__}, ...) are substituted with current timestamps at
 * test time so reads using {@code find[*][$gte]} work without staleness.
 *
 * <p>
 * The point isn't to exercise every field — it's to lock the wire format we accept from
 * the uploaders that real users run.
 */
@SpringBootTest(properties = "nightscout.api-secret=test-secret")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class UploaderPayloadTests {

	@Autowired
	MockMvc mockMvc;

	private static final String ADMIN_HEADER = sha1("test-secret");

	// 2020-01-01T00:00:00Z — see DataIntegrityTests for the rationale.
	private static final long EPOCH_2020 = 1577836800000L;

	/* ---------- xDrip+ ---------- */

	@Test
	void xdripEntriesPostRoundTrip() throws Exception {
		// Fixed pre-2023 anchor; tests share a single testcontainer DB and we must
		// avoid polluting "latest entry" queries in sibling test classes.
		long now = EPOCH_2020 + (long) (Math.random() * 1_000_000);
		String body = renderPayload("payloads/xdrip/entries-post.json", now);

		this.mockMvc
			.perform(post("/api/v1/entries.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0]._id").exists())
			.andExpect(jsonPath("$[0].sgv").value(142))
			.andExpect(jsonPath("$[0].direction").value("Flat"));

		// Read back via the standard sgv endpoint
		this.mockMvc.perform(get("/api/v1/entries/sgv.json").param("count", "10").header("api-secret", ADMIN_HEADER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.sgv==142)]").exists());
	}

	/* ---------- AAPS ---------- */

	@Test
	void aapsEntriesPostRoundTrip() throws Exception {
		// Fixed pre-2023 anchor; tests share a single testcontainer DB and we must
		// avoid polluting "latest entry" queries in sibling test classes.
		long now = EPOCH_2020 + (long) (Math.random() * 1_000_000);
		String body = renderPayload("payloads/aaps/entries-post.json", now);

		this.mockMvc
			.perform(post("/api/v1/entries.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].sgv").value(118))
			.andExpect(jsonPath("$[0].device").value("AndroidAPS-3.2.0"));
	}

	@Test
	void aapsTreatmentsPostRoundTripPreservesPolymorphicFields() throws Exception {
		// Fixed pre-2023 anchor; tests share a single testcontainer DB and we must
		// avoid polluting "latest entry" queries in sibling test classes.
		long now = EPOCH_2020 + (long) (Math.random() * 1_000_000);
		String body = renderPayload("payloads/aaps/treatments-post.json", now);

		this.mockMvc
			.perform(post("/api/v1/treatments.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0]._id").exists());

		// Verify Meal Bolus came back through the find filter that AAPS uses
		String sinceIso = Instant.ofEpochMilli(now - 1000).toString();
		this.mockMvc
			.perform(get("/api/v1/treatments.json").param("find[created_at][$gte]", sinceIso)
				.header("api-secret", ADMIN_HEADER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.eventType=='Meal Bolus' && @.carbs==45 && @.insulin==4.2)]").exists())
			.andExpect(jsonPath("$[?(@.eventType=='Temp Basal' && @.duration==30)]").exists());
	}

	@Test
	void aapsDevicestatusPostRoundTrip() throws Exception {
		// Fixed pre-2023 anchor; tests share a single testcontainer DB and we must
		// avoid polluting "latest entry" queries in sibling test classes.
		long now = EPOCH_2020 + (long) (Math.random() * 1_000_000);
		String body = renderPayload("payloads/aaps/devicestatus-post.json", now);

		this.mockMvc
			.perform(post("/api/v1/devicestatus.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0]._id").exists());
	}

	/* ---------- Loop (LoopKit) ---------- */

	@Test
	void loopDevicestatusPostRoundTrip() throws Exception {
		// Fixed pre-2023 anchor; tests share a single testcontainer DB and we must
		// avoid polluting "latest entry" queries in sibling test classes.
		long now = EPOCH_2020 + (long) (Math.random() * 1_000_000);
		String body = renderPayload("payloads/loop/devicestatus-post.json", now);

		this.mockMvc
			.perform(post("/api/v1/devicestatus.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0]._id").exists());
	}

	@Test
	void loopTreatmentsPostPreservesSyncIdentifier() throws Exception {
		// Fixed pre-2023 anchor; tests share a single testcontainer DB and we must
		// avoid polluting "latest entry" queries in sibling test classes.
		long now = EPOCH_2020 + (long) (Math.random() * 1_000_000);
		String syncId = "loop-sync-" + now;
		String body = renderPayload("payloads/loop/treatments-post.json", now).replace("__SYNC_ID__", syncId);

		this.mockMvc
			.perform(post("/api/v1/treatments.json").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0]._id").exists())
			.andExpect(jsonPath("$[0].syncIdentifier").value(syncId))
			.andExpect(jsonPath("$[0].insulinType").value("novolog"));

		String sinceIso = Instant.ofEpochMilli(now - 1000).toString();
		this.mockMvc
			.perform(get("/api/v1/treatments.json").param("find[created_at][$gte]", sinceIso)
				.header("api-secret", ADMIN_HEADER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.syncIdentifier=='" + syncId + "')]").exists());
	}

	/* ---------- helpers ---------- */

	private static String renderPayload(String classpath, long now) throws Exception {
		String raw = new String(new ClassPathResource(classpath).getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);
		String iso = Instant.ofEpochMilli(now).toString();
		String isoPlus1 = Instant.ofEpochMilli(now + 1000).toString();
		return raw.replace("__NOW_ISO__", iso)
			.replace("__NOW_PLUS_1_ISO__", isoPlus1)
			.replace("__NOW_PLUS_1__", String.valueOf(now + 1000))
			.replace("__NOW__", String.valueOf(now));
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
