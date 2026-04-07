package se.ohdeere.nightscout.service.admin;

import java.time.Instant;
import java.util.UUID;

import se.ohdeere.nightscout.storage.admin.ApiKeyRepository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sibling bean so that {@link AdminService#resolveToken} can fire-and-forget the
 * {@code last_used_at} update without blocking the request thread. Lives in its own class
 * because Spring's {@code @Async} only works through the proxy, and self-invocation from
 * within {@code AdminService} would silently run inline.
 */
@Service
class ApiKeyTouchService {

	private final ApiKeyRepository keys;

	ApiKeyTouchService(ApiKeyRepository keys) {
		this.keys = keys;
	}

	@Async
	void touchAsync(UUID id) {
		this.keys.touchLastUsed(id, Instant.now());
	}

}
