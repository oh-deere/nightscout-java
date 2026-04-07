package se.ohdeere.nightscout.api.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import se.ohdeere.nightscout.TestcontainersConfig;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "nightscout.api-secret=test-secret")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AdminControllerTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	private static final String ADMIN_HEADER = sha1("test-secret");

	@Test
	void adminEndpointsRequireAdmin() throws Exception {
		this.mockMvc.perform(get("/api/v2/admin/keys")).andExpect(status().isUnauthorized());
	}

	@Test
	void createListAndRevokeApiKey() throws Exception {
		// Create
		MvcResult created = this.mockMvc
			.perform(post("/api/v2/admin/keys").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"xdrip-1","scope":"write"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").exists())
			.andExpect(jsonPath("$.name").value("xdrip-1"))
			.andExpect(jsonPath("$.scope").value("write"))
			.andReturn();

		JsonNode body = this.objectMapper.readTree(created.getResponse().getContentAsString());
		String plaintext = body.get("token").asText();
		String id = body.get("id").asText();
		assertThat(plaintext).hasSize(48); // 24 bytes hex

		// Use the new key to authenticate (entries read endpoint)
		this.mockMvc.perform(get("/api/v1/entries.json").param("token", plaintext)).andExpect(status().isOk());

		// List should not return the plaintext
		this.mockMvc.perform(get("/api/v2/admin/keys").header("api-secret", ADMIN_HEADER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.name=='xdrip-1')].token").doesNotExist());

		// Revoke
		this.mockMvc.perform(delete("/api/v2/admin/keys/" + id).header("api-secret", ADMIN_HEADER))
			.andExpect(status().isNoContent());

		// Revoked token no longer authenticates
		this.mockMvc.perform(get("/api/v1/entries.json").param("token", plaintext))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void putAndDeleteRuntimeSetting() throws Exception {
		this.mockMvc
			.perform(put("/api/v2/admin/settings/units").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("\"mmol/l\""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.key").value("units"))
			.andExpect(jsonPath("$.value").value("\"mmol/l\""));

		this.mockMvc.perform(get("/api/v2/admin/settings/units").header("api-secret", ADMIN_HEADER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.value").value("\"mmol/l\""));

		// Update
		this.mockMvc
			.perform(put("/api/v2/admin/settings/units").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("\"mg/dl\""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.value").value("\"mg/dl\""));

		this.mockMvc.perform(delete("/api/v2/admin/settings/units").header("api-secret", ADMIN_HEADER))
			.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/v2/admin/settings/units").header("api-secret", ADMIN_HEADER))
			.andExpect(status().isNotFound());
	}

	@Test
	void runtimeSettingOverridesStatusEndpoint() throws Exception {
		// override units via runtime setting
		this.mockMvc
			.perform(put("/api/v2/admin/settings/units").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("\"mmol/l\""))
			.andExpect(status().isOk());

		this.mockMvc.perform(get("/api/v1/status.json"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.settings.units").value("mmol/l"));

		// override a threshold and verify
		this.mockMvc
			.perform(put("/api/v2/admin/settings/thresholds.bgHigh").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("250"))
			.andExpect(status().isOk());

		this.mockMvc.perform(get("/api/v1/status.json"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.settings.thresholds.bgHigh").value(250));

		// cleanup
		this.mockMvc.perform(delete("/api/v2/admin/settings/units").header("api-secret", ADMIN_HEADER));
		this.mockMvc.perform(delete("/api/v2/admin/settings/thresholds.bgHigh").header("api-secret", ADMIN_HEADER));
	}

	@Test
	void auditLogCapturesKeyAndSettingsWrites() throws Exception {
		this.mockMvc
			.perform(post("/api/v2/admin/keys").header("api-secret", ADMIN_HEADER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"audited","scope":"read"}"""))
			.andExpect(status().isOk());

		this.mockMvc.perform(get("/api/v2/admin/audit?limit=10").header("api-secret", ADMIN_HEADER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.action=='key.create' && @.target=='audited')]").exists());
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
