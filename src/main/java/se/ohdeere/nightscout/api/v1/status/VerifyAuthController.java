package se.ohdeere.nightscout.api.v1.status;

import java.util.Map;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.service.auth.NightscoutAuth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class VerifyAuthController {

	@GetMapping("/api/v1/verifyauth")
	Map<String, Object> verifyAuth() {
		NightscoutAuth auth = AuthHelper.current();
		if (auth == NightscoutAuth.ANONYMOUS) {
			return Map.of("message", "UNAUTHORIZED", "status", 401);
		}
		return Map.of("message", "OK", "status", 200, "sub", auth.subject(), "permissions", auth.permissions(), "admin",
				auth.admin());
	}

}
