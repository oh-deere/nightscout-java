package se.ohdeere.nightscout.api.auth;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import se.ohdeere.nightscout.service.auth.AuthService;
import se.ohdeere.nightscout.service.auth.NightscoutAuth;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class NightscoutAuthFilter extends OncePerRequestFilter {

	private final AuthService authService;

	NightscoutAuthFilter(AuthService authService) {
		this.authService = authService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String apiSecretHeader = request.getHeader("api-secret");
		String secretParam = request.getParameter("secret");
		String tokenParam = request.getParameter("token");
		String authHeader = request.getHeader("Authorization");
		String bearerToken = tokenParam;
		if (bearerToken == null && authHeader != null && authHeader.startsWith("Bearer ")) {
			bearerToken = authHeader.substring(7);
		}

		Optional<NightscoutAuth> auth = this.authService.authenticate(apiSecretHeader, secretParam, bearerToken);

		if (auth.isPresent()) {
			NightscoutAuth nsAuth = auth.get();
			List<SimpleGrantedAuthority> authorities = nsAuth.admin()
					? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
					: List.of(new SimpleGrantedAuthority("ROLE_USER"));
			UsernamePasswordAuthenticationToken springAuth = new UsernamePasswordAuthenticationToken(nsAuth, null,
					authorities);
			SecurityContextHolder.getContext().setAuthentication(springAuth);
		}

		filterChain.doFilter(request, response);
	}

}
