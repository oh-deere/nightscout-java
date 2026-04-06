package se.ohdeere.nightscout.storage.profiles;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface ProfileRepository extends CrudRepository<Profile, UUID> {

	@Query("SELECT * FROM profiles ORDER BY created_at DESC")
	List<Profile> findAllOrdered();

	@Query("SELECT * FROM profiles ORDER BY created_at DESC LIMIT 1")
	Profile findCurrent();

}
