package se.ohdeere.nightscout.service.auth.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import se.ohdeere.nightscout.NightscoutProperties;
import se.ohdeere.nightscout.service.auth.AuthService;
import se.ohdeere.nightscout.service.auth.NightscoutAuth;

import org.springframework.stereotype.Service;

@Service
class AuthServiceImpl implements AuthService {

	private final String apiSecretHash;

	private final SecretKey jwtKey;

	AuthServiceImpl(NightscoutProperties properties) {
		String secret = properties.apiSecret();
		this.apiSecretHash = (secret != null && !secret.isBlank()) ? sha1(secret) : "";
		// Use API_SECRET as JWT signing key (same as upstream Nightscout)
		byte[] keyBytes = (secret != null && !secret.isBlank()) ? secret.getBytes(StandardCharsets.UTF_8)
				: "nightscout-default-key".getBytes(StandardCharsets.UTF_8);
		// Pad to at least 256 bits for HMAC-SHA256
		if (keyBytes.length < 32) {
			byte[] padded = new byte[32];
			System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
			keyBytes = padded;
		}
		this.jwtKey = Keys.hmacShaKeyFor(keyBytes);
	}

	@Override
	public Optional<NightscoutAuth> authenticate(String apiSecretHeader, String secretParam, String bearerToken) {
		// 1. Check api-secret header (SHA-1 hash of API_SECRET)
		if (apiSecretHeader != null && !apiSecretHeader.isBlank()) {
			if (this.apiSecretHash.equalsIgnoreCase(apiSecretHeader)) {
				return Optional.of(NightscoutAuth.adminAuth("api-secret"));
			}
			// Also accept the raw secret itself
			if (this.apiSecretHash.equalsIgnoreCase(sha1(apiSecretHeader))) {
				return Optional.of(NightscoutAuth.adminAuth("api-secret"));
			}
			return Optional.empty();
		}

		// 2. Check ?secret= query param
		if (secretParam != null && !secretParam.isBlank()) {
			if (this.apiSecretHash.equalsIgnoreCase(secretParam)) {
				return Optional.of(NightscoutAuth.adminAuth("api-secret"));
			}
			return Optional.empty();
		}

		// 3. Check JWT token (from ?token= or Authorization: Bearer)
		String token = bearerToken;
		if (token != null && !token.isBlank()) {
			return parseJwt(token);
		}

		return Optional.empty();
	}

	@Override
	public String issueToken(String subject, List<String> permissions, long expirySeconds) {
		return Jwts.builder()
			.subject(subject)
			.claim("permissions", permissions)
			.issuedAt(Date.from(Instant.now()))
			.expiration(Date.from(Instant.now().plusSeconds(expirySeconds)))
			.signWith(this.jwtKey)
			.compact();
	}

	private Optional<NightscoutAuth> parseJwt(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(this.jwtKey).build().parseSignedClaims(token).getPayload();
			String subject = claims.getSubject();
			@SuppressWarnings("unchecked")
			List<String> permissions = claims.get("permissions", List.class);
			if (permissions == null) {
				permissions = List.of();
			}
			boolean admin = permissions.stream().anyMatch(p -> p.equals("*:*:*") || p.equals("*"));
			return Optional.of(new NightscoutAuth(subject, permissions, admin));
		}
		catch (Exception ex) {
			return Optional.empty();
		}
	}

	static String sha1(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException("SHA-1 not available", ex);
		}
	}

}
