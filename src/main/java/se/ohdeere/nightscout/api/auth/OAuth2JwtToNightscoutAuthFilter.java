package se.ohdeere.nightscout.api.auth;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import se.ohdeere.nightscout.service.auth.NightscoutAuth;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bridges Spring Security's OAuth2 {@link JwtAuthenticationToken} into our
 * {@link NightscoutAuth} principal so the rest of the app (controllers,
 * {@link AuthHelper#requirePermission}) doesn't need to know which auth method was used.
 *
 * Runs <b>after</b> the OAuth2 resource server filter populates the security context. If
 * the principal is already a {@code NightscoutAuth} (because the api-secret filter set
 * it), we leave it alone. Only registered when a {@link JwtDecoder} bean is present, i.e.
 * when {@code nightscout.oauth.enabled=true}.
 */
public class OAuth2JwtToNightscoutAuthFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication current = SecurityContextHolder.getContext().getAuthentication();
		if (current instanceof JwtAuthenticationToken jwtAuth) {
			NightscoutAuth nightscoutAuth = toNightscoutAuth(jwtAuth.getToken());
			SimpleGrantedAuthority role = new SimpleGrantedAuthority(
					nightscoutAuth.admin() ? "ROLE_ADMIN" : "ROLE_USER");
			SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(nightscoutAuth, jwtAuth.getCredentials(),
						List.of(role)));
		}
		filterChain.doFilter(request, response);
	}

	private NightscoutAuth toNightscoutAuth(Jwt jwt) {
		String subject = jwt.getSubject() != null ? jwt.getSubject() : "oauth-user";
		// Look for nightscout-style permissions in the JWT first; fall back to the
		// standard OAuth scope claim.
		List<String> permissions = jwt.getClaimAsStringList("permissions");
		if (permissions == null || permissions.isEmpty()) {
			List<String> scopes = jwt.getClaimAsStringList("scope");
			if (scopes == null) {
				String scopeStr = jwt.getClaimAsString("scope");
				if (scopeStr != null && !scopeStr.isBlank()) {
					scopes = List.of(scopeStr.split("\\s+"));
				}
			}
			permissions = scopes != null ? scopes : List.of();
		}
		boolean admin = permissions.stream().anyMatch(p -> p.equals("*:*:*") || p.equals("admin"));
		return new NightscoutAuth(subject, permissions, admin);
	}

}
