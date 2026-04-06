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

	@GetMapping("/api/v3/authorization/request/{accessToken}")
	ResponseEntity<Map<String, Object>> exchangeToken(@PathVariable String accessToken) {
		return this.authService.authenticate(null, null, accessToken).map(auth -> {
			String jwt = this.authService.issueToken(auth.subject(), auth.permissions(), 3600);
			return ResponseEntity.ok(Map.<String, Object>of("token", jwt, "sub", auth.subject()));
		}).orElse(ResponseEntity.status(401).body(Map.of("message", "Invalid token")));
	}

}
