package se.ohdeere.nightscout.api.auth;

import se.ohdeere.nightscout.service.auth.NightscoutAuth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthHelper {

	private AuthHelper() {
	}

	public static NightscoutAuth current() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof NightscoutAuth nsAuth) {
			return nsAuth;
		}
		return NightscoutAuth.ANONYMOUS;
	}

	public static void requirePermission(String collection, String action) {
		NightscoutAuth auth = current();
		if (!auth.hasPermission(collection, action)) {
			throw new org.springframework.security.access.AccessDeniedException(
					"Missing permission: api:" + collection + ":" + action);
		}
	}

}
