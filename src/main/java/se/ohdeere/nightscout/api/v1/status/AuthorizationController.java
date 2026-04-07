package se.ohdeere.nightscout.api.v1.status;

import java.util.List;
import java.util.Map;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.service.auth.AuthService;
import se.ohdeere.nightscout.service.auth.NightscoutAuth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuthorizationController {

	private final AuthService authService;

	AuthorizationController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/api/v2/authorization/request")
	ResponseEntity<Map<String, Object>> requestAuthorization(@RequestBody Map<String, String> body) {
		NightscoutAuth auth = AuthHelper.current();
		if (auth == NightscoutAuth.ANONYMOUS) {
			return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
		}
		String token = this.authService.issueToken(auth.subject(), auth.permissions(), 86400);
		return ResponseEntity.ok(Map.of("token", token, "sub", auth.subject(), "permissionGroups", auth.permissions()));
	}

	@GetMapping({ "/api/v2/authorization/request/{accessToken}", "/api/v3/authorization/request/{accessToken}" })
	ResponseEntity<Map<String, Object>> exchangeToken(@PathVariable String accessToken) {
		// Try as api-secret hash first (40 hex chars = SHA-1), then as JWT
		var auth = this.authService.authenticate(accessToken, null, null);
		if (auth.isEmpty()) {
			auth = this.authService.authenticate(null, accessToken, null);
		}
		if (auth.isEmpty()) {
			auth = this.authService.authenticate(null, null, accessToken);
		}
		return auth.map(a -> {
			String jwt = this.authService.issueToken(a.subject(), a.permissions(), 3600);
			long iat = System.currentTimeMillis() / 1000;
			long exp = iat + 3600;
			return ResponseEntity.<Map<String, Object>>ok(Map.of("token", jwt, "sub", a.subject(), "permissionGroups",
					a.permissions(), "iat", iat, "exp", exp));
		}).orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Invalid token")));
	}

}
