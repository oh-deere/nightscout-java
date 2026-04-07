package se.ohdeere.nightscout.storage.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface AuditEntryRepository extends CrudRepository<AuditEntry, UUID> {

	@Query("SELECT * FROM audit_log ORDER BY occurred_at DESC LIMIT :limit")
	List<AuditEntry> findRecent(int limit);

	@Query("SELECT * FROM audit_log WHERE target = :target ORDER BY occurred_at DESC LIMIT :limit")
	List<AuditEntry> findByTarget(String target, int limit);

}
