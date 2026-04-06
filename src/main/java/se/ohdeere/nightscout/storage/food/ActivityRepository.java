package se.ohdeere.nightscout.storage.food;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface ActivityRepository extends CrudRepository<Activity, UUID> {

	@Query("SELECT * FROM activity ORDER BY created_at DESC LIMIT :count")
	List<Activity> findLatest(int count);

}
