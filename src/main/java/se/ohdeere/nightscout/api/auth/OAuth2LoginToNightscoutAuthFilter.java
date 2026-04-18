package se.ohdeere.nightscout.api.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import se.ohdeere.nightscout.service.auth.NightscoutAuth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bridges Spring Security's session-based {@link OAuth2AuthenticationToken} (the result
 * of a successful {@code oauth2Login} browser flow) into our {@link NightscoutAuth}
 * principal so {@code AuthHelper.requirePermission} works the same way for OAuth-logged
 * users as it does for api-secret callers.
 *
 * <p>
 * This is the session/login counterpart to {@link OAuth2JwtToNightscoutAuthFilter}, which
 * handles the stateless resource-server Bearer flow. Both filters are idempotent — if a
 * {@code NightscoutAuth} is already in the security context (e.g. the api-secret filter
 * ran first) we leave it alone.
 *
 * <p>
 * Authorization mapping is intentionally simple: the {@code nightscout.oauth.admins}
 * property holds a comma-separated allowlist of email claims (or sub UUIDs); anyone in
 * the list gets {@code adminAuth}, anyone else gets an anonymous-equivalent
 * {@link NightscoutAuth} so the downstream permission check denies them.
 */
public class OAuth2LoginToNightscoutAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginToNightscoutAuthFilter.class);

	private final Set<String> adminAllowlist;

	public OAuth2LoginToNightscoutAuthFilter(String adminsCsv) {
		if (adminsCsv == null || adminsCsv.isBlank()) {
			this.adminAllowlist = Set.of();
		}
		else {
			this.adminAllowlist = new HashSet<>(Arrays.asList(adminsCsv.split("\\s*,\\s*")));
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication current = SecurityContextHolder.getContext().getAuthentication();
		if (current instanceof OAuth2AuthenticationToken oauthToken) {
			NightscoutAuth nightscoutAuth = toNightscoutAuth(oauthToken.getPrincipal());
			SimpleGrantedAuthority role = new SimpleGrantedAuthority(
					nightscoutAuth.admin() ? "ROLE_ADMIN" : "ROLE_USER");
			SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(nightscoutAuth, oauthToken.getCredentials(),
						List.of(role)));
		}
		filterChain.doFilter(request, response);
	}

	private NightscoutAuth toNightscoutAuth(OAuth2User user) {
		String email = stringClaim(user, "email");
		String sub = stringClaim(user, "sub");
		String identity = (email != null) ? email : (sub != null ? sub : "oauth-user");

		boolean isAdmin = (email != null && this.adminAllowlist.contains(email))
				|| (sub != null && this.adminAllowlist.contains(sub));
		log.info("OAuth login bridge: email={} sub={} allowlist={} admin={}", email, sub, this.adminAllowlist, isAdmin);
		if (isAdmin) {
			return NightscoutAuth.adminAuth(identity);
		}
		return new NightscoutAuth(identity, List.of(), false);
	}

	private static String stringClaim(OAuth2User user, String name) {
		Object value;
		if (user instanceof OidcUser oidc) {
			value = oidc.getClaims().get(name);
		}
		else {
			value = user.getAttributes().get(name);
		}
		return (value instanceof String s && !s.isBlank()) ? s : null;
	}

}
