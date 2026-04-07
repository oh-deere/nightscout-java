package se.ohdeere.nightscout.docs;

import se.ohdeere.nightscout.TestcontainersConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base for Spring RestDocs documentation tests. Boots a real Spring Boot context with
 * Testcontainers PostgreSQL so the docs reflect the actual API behavior, not stubs.
 *
 * Snippets are generated under {@code target/generated-snippets/{snippet-name}/} and
 * pulled into {@code src/docs/asciidoc/index.adoc} during the {@code prepare-package}
 * phase by the Asciidoctor plugin.
 */
@ExtendWith({ RestDocumentationExtension.class, SpringExtension.class })
@SpringBootTest(properties = "nightscout.api-secret=docs-test-secret")
@Import(TestcontainersConfig.class)
public abstract class AbstractDocsTest {

	@Autowired
	private WebApplicationContext context;

	protected MockMvc mockMvc;

	protected static final String API_SECRET = "docs-test-secret";

	protected static final String API_SECRET_HASH = sha1Hex(API_SECRET);

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.context);
		builder.apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentation));
		builder.apply(SecurityMockMvcConfigurers.springSecurity());
		this.mockMvc = builder.build();
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
