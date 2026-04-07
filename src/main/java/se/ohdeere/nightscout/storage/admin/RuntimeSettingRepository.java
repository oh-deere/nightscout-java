package se.ohdeere.nightscout.storage.admin;

import java.time.Instant;
import java.util.List;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface RuntimeSettingRepository extends CrudRepository<RuntimeSetting, String> {

	@Query("SELECT * FROM runtime_settings ORDER BY key")
	List<RuntimeSetting> findAllOrdered();

	@Modifying
	@Query("""
			INSERT INTO runtime_settings (key, value, updated_at, updated_by)
			VALUES (:key, :value::jsonb, :updatedAt, :updatedBy)
			ON CONFLICT (key) DO UPDATE SET
			    value = EXCLUDED.value,
			    updated_at = EXCLUDED.updated_at,
			    updated_by = EXCLUDED.updated_by
			""")
	void upsert(String key, JsonValue value, Instant updatedAt, String updatedBy);

}
