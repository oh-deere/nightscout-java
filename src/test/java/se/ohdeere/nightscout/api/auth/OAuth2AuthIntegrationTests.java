package se.ohdeere.nightscout.api.auth;

import java.time.Instant;
import java.util.List;

import se.ohdeere.nightscout.TestcontainersConfig;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that when {@code nightscout.oauth.enabled=true}, an inbound Bearer JWT
 * validated against a (mocked) {@link JwtDecoder} successfully authenticates the request.
 *
 * The mock decoder accepts any token and returns a JWT with the {@code admin} permission,
 * so we can hit a protected endpoint without needing a live OAuth issuer.
 */
@SpringBootTest(properties = { "nightscout.oauth.enabled=true",
		"nightscout.oauth.jwk-set-uri=http://localhost:0/never-called", "nightscout.api-secret=oauth-test-secret" })
@AutoConfigureMockMvc
@Import({ TestcontainersConfig.class, OAuth2AuthIntegrationTests.MockDecoderConfig.class })
class OAuth2AuthIntegrationTests {

	@Autowired
	MockMvc mockMvc;

	@Test
	void apiSecretAuthStillWorks() throws Exception {
		String hash = sha1Hex("oauth-test-secret");
		this.mockMvc.perform(get("/api/v1/entries.json").header("api-secret", hash)).andExpect(status().isOk());
	}

	@Test
	void bearerJwtAuthGrantsAccess() throws Exception {
		this.mockMvc.perform(get("/api/v1/entries.json").header("Authorization", "Bearer fake-jwt-token"))
			.andExpect(status().isOk());
	}

	@Test
	void noCredentialsReturns401() throws Exception {
		this.mockMvc.perform(get("/api/v1/entries.json")).andExpect(status().isUnauthorized());
	}

	@Test
	void publicStatusEndpointWorks() throws Exception {
		this.mockMvc.perform(get("/api/v1/status.json")).andExpect(status().isOk());
	}

	@TestConfiguration
	static class MockDecoderConfig {

		/**
		 * Replace the real {@link JwtDecoder} with one that accepts any token and returns
		 * a synthetic JWT carrying admin permissions.
		 */
		@Bean
		@Primary
		JwtDecoder mockJwtDecoder() {
			JwtDecoder decoder = Mockito.mock(JwtDecoder.class);
			Jwt jwt = Jwt.withTokenValue("fake-jwt-token")
				.header("alg", "RS256")
				.subject("integration-test-user")
				.claim("permissions", List.of("*:*:*"))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();
			Mockito.when(decoder.decode(Mockito.anyString())).thenReturn(jwt);
			return decoder;
		}

	}

	private static String sha1Hex(String input) {
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
