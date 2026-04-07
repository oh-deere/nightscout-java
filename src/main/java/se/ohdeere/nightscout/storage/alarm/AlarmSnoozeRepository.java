package se.ohdeere.nightscout.storage.alarm;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface AlarmSnoozeRepository extends CrudRepository<AlarmSnooze, String> {

	@Query("SELECT * FROM alarm_snoozes WHERE snoozed_until > :now")
	List<AlarmSnooze> findActive(Instant now);

	@Modifying
	@Query("""
			INSERT INTO alarm_snoozes (type, snoozed_until, snoozed_by, snoozed_at)
			VALUES (:type, :until, :by, :at)
			ON CONFLICT (type) DO UPDATE SET
			    snoozed_until = EXCLUDED.snoozed_until,
			    snoozed_by = EXCLUDED.snoozed_by,
			    snoozed_at = EXCLUDED.snoozed_at
			""")
	void upsert(String type, Instant until, String by, Instant at);

	@Modifying
	@Query("DELETE FROM alarm_snoozes WHERE snoozed_until <= :now")
	int deleteExpired(Instant now);

}
