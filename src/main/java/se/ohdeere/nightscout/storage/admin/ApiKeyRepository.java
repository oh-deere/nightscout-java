package se.ohdeere.nightscout.storage.admin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface ApiKeyRepository extends CrudRepository<ApiKey, UUID> {

	Optional<ApiKey> findByTokenHash(String tokenHash);

	@Query("SELECT * FROM api_keys ORDER BY created_at DESC")
	List<ApiKey> findAllOrdered();

	@Modifying
	@Query("UPDATE api_keys SET last_used_at = :now WHERE id = :id")
	void touchLastUsed(UUID id, Instant now);

}
