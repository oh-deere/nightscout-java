package se.ohdeere.nightscout.api.v1.entries;

import se.ohdeere.nightscout.TestcontainersConfig;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "nightscout.api-secret=test-secret")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class EntriesControllerTests {

	@Autowired
	MockMvc mockMvc;

	@Test
	void statusEndpointIsPublic() throws Exception {
		this.mockMvc.perform(get("/api/v1/status.json"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void getEntriesRequiresAuth() throws Exception {
		this.mockMvc.perform(get("/api/v1/entries.json")).andExpect(status().isUnauthorized());
	}

	@Test
	void postAndGetEntriesWithApiSecret() throws Exception {
		// SHA-1 of "test-secret"
		String apiSecretHash = sha1("test-secret");

		// Post a single SGV entry
		this.mockMvc
			.perform(post("/api/v1/entries.json").header("api-secret", apiSecretHash)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						[{
							"type": "sgv",
							"date": 1700000010000,
							"dateString": "2023-11-14T22:13:30.000Z",
							"sysTime": "2023-11-14T22:13:30.000Z",
							"sgv": 145,
							"direction": "FortyFiveUp",
							"device": "nightscout-librelink-up",
							"noise": 1
						}]
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0]._id").exists())
			.andExpect(jsonPath("$[0].sgv").value(145))
			.andExpect(jsonPath("$[0].direction").value("FortyFiveUp"));

		// Get entries
		this.mockMvc.perform(get("/api/v1/entries.json").header("api-secret", apiSecretHash))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].sgv").value(145));

		// Get current
		this.mockMvc.perform(get("/api/v1/entries/current.json").header("api-secret", apiSecretHash))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].sgv").value(145));

		// Get sgv
		this.mockMvc.perform(get("/api/v1/entries/sgv.json").header("api-secret", apiSecretHash))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].type").value("sgv"));
	}

	@Test
	void postWithInvalidSecretReturns401() throws Exception {
		this.mockMvc
			.perform(post("/api/v1/entries.json").header("api-secret", "wrong-hash")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						[{"type": "sgv", "date": 1700000000000, "sgv": 100}]
						"""))
			.andExpect(status().isUnauthorized());
	}

	private static String sha1(String input) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
