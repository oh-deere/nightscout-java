package se.ohdeere.nightscout.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Wires up the OAuth2 {@link JwtDecoder} only when {@code nightscout.oauth.enabled=true}.
 *
 * Two ways to provide JWT validation keys, in order of preference:
 *
 * <ol>
 * <li>{@code nightscout.oauth.jwk-set-uri} — direct URL to the issuer's JWKS endpoint
 * (faster startup, no metadata fetch)</li>
 * <li>{@code nightscout.oauth.issuer-uri} — issuer base URL; we discover the JWKS via the
 * standard OIDC well-known metadata document</li>
 * </ol>
 *
 * Disabled by default so local dev and the e2e suite don't need a reachable issuer.
 */
@Configuration
@ConditionalOnProperty(name = "nightscout.oauth.enabled", havingValue = "true")
class OAuthConfig {

	@Value("${nightscout.oauth.issuer-uri:}")
	private String issuerUri;

	@Value("${nightscout.oauth.jwk-set-uri:}")
	private String jwkSetUri;

	@Bean
	JwtDecoder jwtDecoder() {
		if (this.jwkSetUri != null && !this.jwkSetUri.isBlank()) {
			return NimbusJwtDecoder.withJwkSetUri(this.jwkSetUri).build();
		}
		if (this.issuerUri != null && !this.issuerUri.isBlank()) {
			return NimbusJwtDecoder.withIssuerLocation(this.issuerUri).build();
		}
		throw new IllegalStateException("nightscout.oauth.enabled=true but neither nightscout.oauth.jwk-set-uri "
				+ "nor nightscout.oauth.issuer-uri is configured");
	}

	@Bean
	OAuth2JwtToNightscoutAuthFilter oauthBridgeFilter() {
		return new OAuth2JwtToNightscoutAuthFilter();
	}

}
