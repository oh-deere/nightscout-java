package se.ohdeere.nightscout.storage.alarm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface AlarmHistoryRepository extends CrudRepository<AlarmHistoryEntry, UUID> {

	@Query("SELECT * FROM alarm_history ORDER BY occurred_at DESC LIMIT :limit")
	List<AlarmHistoryEntry> findRecent(int limit);

	@Query("SELECT * FROM alarm_history WHERE occurred_at >= :since ORDER BY occurred_at DESC")
	List<AlarmHistoryEntry> findSince(Instant since);

}
