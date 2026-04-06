package se.ohdeere.nightscout.storage.entries;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface EntryRepository extends CrudRepository<Entry, UUID> {

	@Query("SELECT * FROM entries ORDER BY date_ms DESC LIMIT :count")
	List<Entry> findLatest(int count);

	@Query("SELECT * FROM entries WHERE type = :type ORDER BY date_ms DESC LIMIT :count")
	List<Entry> findLatestByType(String type, int count);

	@Query("SELECT * FROM entries WHERE type = :type ORDER BY date_ms DESC LIMIT 1")
	Entry findCurrentByType(String type);

	@Query("SELECT * FROM entries WHERE date_ms >= :from AND date_ms <= :to ORDER BY date_ms DESC")
	List<Entry> findByDateRange(long from, long to);

	@Query("SELECT * FROM entries WHERE type = :type AND date_ms >= :from ORDER BY date_ms DESC")
	List<Entry> findByTypeAndDateFrom(String type, long from);

}
