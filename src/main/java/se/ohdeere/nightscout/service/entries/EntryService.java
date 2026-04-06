package se.ohdeere.nightscout.service.entries;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import se.ohdeere.nightscout.storage.entries.Entry;

public interface EntryService {

	List<Entry> findLatest(int count);

	List<Entry> findLatestByType(String type, int count);

	Optional<Entry> findCurrent();

	List<Entry> findByDateRange(long from, long to);

	List<Entry> findByTypeAndDateFrom(String type, long from);

	List<Entry> saveAll(List<Entry> entries);

	Optional<Entry> findById(UUID id);

	void deleteById(UUID id);

}
