package se.ohdeere.nightscout.service.entries.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.observability.NightscoutMetrics;
import se.ohdeere.nightscout.service.entries.DataUpdateBroadcaster;
import se.ohdeere.nightscout.service.entries.EntryService;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
class EntryServiceImpl implements EntryService {

	private static final Logger LOG = LoggerFactory.getLogger(EntryServiceImpl.class);

	private final EntryRepository entryRepository;

	private final DataUpdateBroadcaster broadcaster;

	private final ObjectProvider<NightscoutMetrics> metrics;

	EntryServiceImpl(EntryRepository entryRepository, DataUpdateBroadcaster broadcaster,
			ObjectProvider<NightscoutMetrics> metrics) {
		this.entryRepository = entryRepository;
		this.broadcaster = broadcaster;
		this.metrics = metrics;
	}

	@Override
	public List<Entry> findLatest(int count) {
		return this.entryRepository.findLatest(Math.min(count, 10000));
	}

	@Override
	public List<Entry> findLatestByType(String type, int count) {
		return this.entryRepository.findLatestByType(type, Math.min(count, 10000));
	}

	@Override
	public Optional<Entry> findCurrent() {
		return Optional.ofNullable(this.entryRepository.findCurrentByType("sgv"));
	}

	@Override
	public List<Entry> findByDateRange(long from, long to) {
		return this.entryRepository.findByDateRange(from, to);
	}

	@Override
	public List<Entry> findByTypeAndDateFrom(String type, long from) {
		return this.entryRepository.findByTypeAndDateFrom(type, from);
	}

	@Override
	public List<Entry> saveAll(List<Entry> entries) {
		List<Entry> saved = new ArrayList<>();
		for (Entry entry : entries) {
			try {
				saved.add(this.entryRepository.save(entry));
			}
			catch (DuplicateKeyException ex) {
				LOG.debug("Duplicate entry skipped: type={}, sysTime={}", entry.type(), entry.sysTime());
			}
		}
		if (!saved.isEmpty()) {
			this.broadcaster.broadcastNewEntries(saved);
			NightscoutMetrics m = this.metrics.getIfAvailable();
			if (m != null) {
				m.recordEntryWritten(saved.size());
			}
		}
		return saved;
	}

	@Override
	public Optional<Entry> findById(UUID id) {
		return this.entryRepository.findById(id);
	}

	@Override
	public void deleteById(UUID id) {
		this.entryRepository.deleteById(id);
	}

}
