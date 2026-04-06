package se.ohdeere.nightscout.storage.food;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface FoodRepository extends CrudRepository<Food, UUID> {

	@Query("SELECT * FROM food ORDER BY created_at DESC LIMIT :count")
	List<Food> findLatest(int count);

}
