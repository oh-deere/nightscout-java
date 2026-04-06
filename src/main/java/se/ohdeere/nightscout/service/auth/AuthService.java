package se.ohdeere.nightscout.service.auth;

import java.util.Optional;

public interface AuthService {

	Optional<NightscoutAuth> authenticate(String apiSecretHash, String token, String bearerToken);

	String issueToken(String subject, java.util.List<String> permissions, long expirySeconds);

}
