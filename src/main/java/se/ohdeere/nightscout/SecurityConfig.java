package se.ohdeere.nightscout;

import java.util.Optional;

import se.ohdeere.nightscout.api.auth.NightscoutAuthFilter;
import se.ohdeere.nightscout.api.auth.OAuth2JwtToNightscoutAuthFilter;
import se.ohdeere.nightscout.api.auth.OAuth2LoginToNightscoutAuthFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
class SecurityConfig {

	@Autowired(required = false)
	private JwtDecoder jwtDecoder;

	@Autowired(required = false)
	private OAuth2JwtToNightscoutAuthFilter oauthBridgeFilter;

	@Autowired(required = false)
	private OAuth2LoginToNightscoutAuthFilter oauthLoginBridgeFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, NightscoutAuthFilter authFilter) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
			.authorizeHttpRequests(auth -> auth
				// Actuator and status are always public
				.requestMatchers("/actuator/**")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/status", "/api/v1/status.json")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/verifyauth")
				.permitAll()
				.requestMatchers("/api/v3/version", "/api/v3/status")
				.permitAll()
				// Token exchange endpoints — credential is the path variable, no header
				// auth
				.requestMatchers("/api/v2/authorization/request/**", "/api/v3/authorization/request/**")
				.permitAll()
				// Static API documentation
				.requestMatchers("/docs/**")
				.permitAll()
				// MCP endpoints (Spring AI starter)
				.requestMatchers("/sse", "/mcp/**")
				.permitAll()
				// OAuth2 login redirect + callback endpoints — Spring Security
				// owns these and they need to be reachable without an existing session.
				.requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**")
				.permitAll()
				// Admin endpoints require ROLE_ADMIN
				.requestMatchers("/api/v2/admin/**")
				.hasRole("ADMIN")
				// Read endpoints require authentication
				.requestMatchers(HttpMethod.GET, "/api/v1/**")
				.authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v3/**")
				.authenticated()
				// Write endpoints require authentication
				.requestMatchers(HttpMethod.POST, "/api/**")
				.authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/**")
				.authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/**")
				.authenticated()
				.requestMatchers(HttpMethod.PATCH, "/api/**")
				.authenticated()
				// Everything else permit
				.anyRequest()
				.permitAll());

		// Optionally enable the OAuth2 resource server alongside the api-secret filter.
		// Both filters run on every request — whichever populates the security context
		// first wins, and downstream code reads NightscoutAuth from the principal.
		if (this.jwtDecoder != null) {
			http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
			// Bridge runs after the OAuth2 BearerTokenAuthenticationFilter populates the
			// JwtAuthenticationToken but before the AuthorizationFilter checks
			// permissions, so the controller sees a NightscoutAuth principal when the
			// controller's AuthHelper.requirePermission runs.
			Optional.ofNullable(this.oauthBridgeFilter)
				.ifPresent(filter -> http.addFilterBefore(filter, AuthorizationFilter.class));

			// Browser login flow. Spring Security handles the redirect, the code
			// exchange, and stores the principal in the HttpSession. After
			// successful login the user lands at "/" (the SPA root) and the bridge
			// filter below converts the OAuth2AuthenticationToken into a NightscoutAuth
			// so AuthHelper.requirePermission keeps working unchanged.
			http.oauth2Login(oauth -> oauth.defaultSuccessUrl("/", true));
			Optional.ofNullable(this.oauthLoginBridgeFilter)
				.ifPresent(filter -> http.addFilterBefore(filter, AuthorizationFilter.class));
		}

		return http.build();
	}

}
