package se.ohdeere.nightscout.storage.entries;

import java.time.Instant;
import java.util.List;

import se.ohdeere.nightscout.TestcontainersConfig;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfig.class)
class EntryRepositoryTests {

	@Autowired
	EntryRepository entryRepository;

	@Test
	void saveAndFindEntry() {
		Entry entry = new Entry(null, "sgv", 1700000000000L, "2023-11-14T22:13:20.000Z", "2023-11-14T22:13:20.000Z",
				120, "Flat", 1, null, null, null, "nightscout-librelink-up", 0, Instant.now());
		Entry saved = this.entryRepository.save(entry);
		assertThat(saved.id()).isNotNull();

		List<Entry> latest = this.entryRepository.findLatest(10);
		assertThat(latest).hasSize(1);
		assertThat(latest.getFirst().sgv()).isEqualTo(120);
		assertThat(latest.getFirst().direction()).isEqualTo("Flat");
	}

	@Test
	void findCurrentByType() {
		this.entryRepository.save(new Entry(null, "sgv", 1700000001000L, "2023-11-14T22:13:21.000Z",
				"2023-11-14T22:13:21.000Z", 115, "FortyFiveUp", 1, null, null, null, "test", 0, Instant.now()));
		this.entryRepository.save(new Entry(null, "sgv", 1700000002000L, "2023-11-14T22:13:22.000Z",
				"2023-11-14T22:13:22.000Z", 130, "SingleUp", 1, null, null, null, "test", 0, Instant.now()));

		Entry current = this.entryRepository.findCurrentByType("sgv");
		assertThat(current).isNotNull();
		assertThat(current.sgv()).isEqualTo(130);
	}

}
