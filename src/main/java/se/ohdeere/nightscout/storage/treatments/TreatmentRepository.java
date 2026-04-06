package se.ohdeere.nightscout.storage.treatments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface TreatmentRepository extends CrudRepository<Treatment, UUID> {

	@Query("SELECT * FROM treatments ORDER BY created_at DESC LIMIT :count")
	List<Treatment> findLatest(int count);

	@Query("SELECT * FROM treatments WHERE created_at >= :since ORDER BY created_at DESC")
	List<Treatment> findSince(Instant since);

	@Query("SELECT * FROM treatments WHERE event_type = :eventType AND created_at >= :since ORDER BY created_at DESC")
	List<Treatment> findByEventTypeSince(String eventType, Instant since);

	@Query("SELECT * FROM treatments WHERE event_type = :eventType ORDER BY created_at DESC LIMIT 1")
	Treatment findLatestByEventType(String eventType);

}
