package se.ohdeere.nightscout.service.auth;

import java.util.List;
import java.util.Optional;

import se.ohdeere.nightscout.TestcontainersConfig;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "nightscout.api-secret=test-secret-123")
@Import(TestcontainersConfig.class)
class AuthServiceTests {

	@Autowired
	AuthService authService;

	@Test
	void authenticateWithValidApiSecretHash() {
		// SHA-1 of "test-secret-123"
		String hash = sha1("test-secret-123");
		Optional<NightscoutAuth> auth = this.authService.authenticate(hash, null, null);
		assertThat(auth).isPresent();
		assertThat(auth.get().admin()).isTrue();
	}

	@Test
	void authenticateWithInvalidSecret() {
		Optional<NightscoutAuth> auth = this.authService.authenticate("wrong-hash", null, null);
		assertThat(auth).isEmpty();
	}

	@Test
	void authenticateWithSecretQueryParam() {
		String hash = sha1("test-secret-123");
		Optional<NightscoutAuth> auth = this.authService.authenticate(null, hash, null);
		assertThat(auth).isPresent();
		assertThat(auth.get().admin()).isTrue();
	}

	@Test
	void issueAndVerifyJwt() {
		String token = this.authService.issueToken("xdrip-uploader", List.of("api:entries:create", "api:entries:read"),
				3600);
		Optional<NightscoutAuth> auth = this.authService.authenticate(null, null, token);
		assertThat(auth).isPresent();
		assertThat(auth.get().subject()).isEqualTo("xdrip-uploader");
		assertThat(auth.get().admin()).isFalse();
		assertThat(auth.get().hasPermission("entries", "create")).isTrue();
		assertThat(auth.get().hasPermission("entries", "read")).isTrue();
		assertThat(auth.get().hasPermission("treatments", "create")).isFalse();
	}

	@Test
	void permissionWildcard() {
		NightscoutAuth auth = NightscoutAuth.adminAuth("admin");
		assertThat(auth.hasPermission("entries", "create")).isTrue();
		assertThat(auth.hasPermission("anything", "anything")).isTrue();
	}

	@Test
	void readablePermission() {
		NightscoutAuth auth = new NightscoutAuth("reader", List.of("api:*:read"), false);
		assertThat(auth.hasPermission("entries", "read")).isTrue();
		assertThat(auth.hasPermission("treatments", "read")).isTrue();
		assertThat(auth.hasPermission("entries", "create")).isFalse();
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
