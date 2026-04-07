package se.ohdeere.nightscout;

import se.ohdeere.nightscout.api.auth.NightscoutAuthFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
class SecurityConfig {

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
				// auth needed
				.requestMatchers("/api/v2/authorization/request/**", "/api/v3/authorization/request/**")
				.permitAll()
				// MCP endpoints (Spring AI starter)
				.requestMatchers("/sse", "/mcp/**")
				.permitAll()
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
		return http.build();
	}

}
