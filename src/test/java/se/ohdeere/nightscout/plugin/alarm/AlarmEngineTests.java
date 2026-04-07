package se.ohdeere.nightscout.plugin.alarm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import se.ohdeere.nightscout.NightscoutProperties.Thresholds;
import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.plugin.ar2.Ar2Plugin;
import se.ohdeere.nightscout.service.admin.EffectiveSettings;
import se.ohdeere.nightscout.storage.alarm.AlarmSnooze;
import se.ohdeere.nightscout.storage.alarm.AlarmSnoozeRepository;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the multi-alarm logic in {@link AlarmEngine}. Uses Mockito so we can
 * drive the dependencies directly without spinning the full Spring context.
 */
class AlarmEngineTests {

	private EntryRepository entries;

	private EffectiveSettings effective;

	private Ar2Plugin ar2;

	private AlarmSnoozeRepository snoozes;

	private final Map<String, AlarmSnooze> snoozeStore = new ConcurrentHashMap<>();

	private AlarmEngine engine;

	@BeforeEach
	void setUp() {
		this.entries = Mockito.mock(EntryRepository.class);
		this.effective = Mockito.mock(EffectiveSettings.class);
		this.ar2 = Mockito.mock(Ar2Plugin.class);
		this.snoozes = Mockito.mock(AlarmSnoozeRepository.class);
		this.snoozeStore.clear();

		when(this.effective.thresholds()).thenReturn(new Thresholds(260, 180, 80, 55));
		when(this.effective.alarmTimeagoWarnMins()).thenReturn(15);
		when(this.effective.alarmTimeagoUrgentMins()).thenReturn(30);
		when(this.effective.units()).thenReturn("mg/dl");
		when(this.effective.deltaWarnMgdl()).thenReturn(15);
		when(this.effective.deltaUrgentMgdl()).thenReturn(25);
		when(this.ar2.calculate()).thenReturn(Optional.empty());

		// In-memory fake of the snooze repository so the unit test stays Spring-free.
		Mockito.doAnswer((InvocationOnMock inv) -> {
			String type = inv.getArgument(0);
			Instant until = inv.getArgument(1);
			String by = inv.getArgument(2);
			Instant at = inv.getArgument(3);
			this.snoozeStore.put(type, new AlarmSnooze(type, until, by, at));
			return null;
		}).when(this.snoozes).upsert(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.any());

		Mockito.when(this.snoozes.findActive(Mockito.any())).thenAnswer(inv -> {
			Instant now = inv.getArgument(0);
			List<AlarmSnooze> active = new ArrayList<>();
			for (AlarmSnooze s : this.snoozeStore.values()) {
				if (s.snoozedUntil().isAfter(now)) {
					active.add(s);
				}
			}
			return active;
		});

		this.engine = new AlarmEngine(this.entries, this.effective, this.ar2, this.snoozes);
	}

	@Test
	void noAlarmsWhenInRange() {
		stubCurrent(120, System.currentTimeMillis());
		stubLatest(List.of(entry(120, System.currentTimeMillis()), entry(118, fiveMinAgo())));

		assertThat(this.engine.computeAlarms()).isEmpty();
	}

	@Test
	void urgentHighFires() {
		long now = System.currentTimeMillis();
		stubCurrent(280, now);
		stubLatest(List.of(entry(280, now), entry(275, fiveMinAgo())));

		List<AlarmEngine.Alarm> alarms = this.engine.computeAlarms();

		assertThat(alarms).extracting(AlarmEngine.Alarm::type).contains("high");
		assertThat(alarms.get(0).level()).isEqualTo(3);
		assertThat(alarms.get(0).title()).isEqualTo("Urgent High");
	}

	@Test
	void rapidRiseFires() {
		long now = System.currentTimeMillis();
		stubCurrent(160, now); // in target range
		stubLatest(List.of(entry(160, now), entry(130, fiveMinAgo()))); // +30 mg/dL

		List<AlarmEngine.Alarm> alarms = this.engine.computeAlarms();

		assertThat(alarms).extracting(AlarmEngine.Alarm::type).contains("rise");
		AlarmEngine.Alarm rise = alarms.stream().filter(a -> "rise".equals(a.type())).findFirst().orElseThrow();
		assertThat(rise.level()).isEqualTo(3); // urgent — delta >= 25
		assertThat(rise.title()).isEqualTo("Rapid Rise");
	}

	@Test
	void predictiveAlarmFromAr2() {
		long now = System.currentTimeMillis();
		stubCurrent(160, now);
		stubLatest(List.of(entry(160, now), entry(158, fiveMinAgo())));
		when(this.ar2.calculate()).thenReturn(Optional.of(PluginResult.withData("ar2", "AR2", "Predicted urgent",
				Map.of("level", "urgent", "forecast", List.of(220.0, 240.0, 260.0)))));

		List<AlarmEngine.Alarm> alarms = this.engine.computeAlarms();

		assertThat(alarms).extracting(AlarmEngine.Alarm::type).contains("predicted");
		AlarmEngine.Alarm predicted = alarms.stream()
			.filter(a -> "predicted".equals(a.type()))
			.findFirst()
			.orElseThrow();
		assertThat(predicted.level()).isEqualTo(3);
	}

	@Test
	void staleDataShortCircuitsOtherAlarms() {
		long staleTimestamp = System.currentTimeMillis() - 35L * 60 * 1000; // 35 min old
		stubCurrent(280, staleTimestamp); // would normally be urgent high
		stubLatest(List.of(entry(280, staleTimestamp), entry(275, staleTimestamp - 5 * 60 * 1000)));

		List<AlarmEngine.Alarm> alarms = this.engine.computeAlarms();

		assertThat(alarms).hasSize(1);
		assertThat(alarms.get(0).type()).isEqualTo("timeago");
		assertThat(alarms.get(0).level()).isEqualTo(3);
	}

	@Test
	void multipleAlarmsAreSortedHighestLevelFirst() {
		long now = System.currentTimeMillis();
		stubCurrent(190, now); // warn-level high
		stubLatest(List.of(entry(190, now), entry(160, fiveMinAgo()))); // +30 → urgent
																		// rise

		List<AlarmEngine.Alarm> alarms = this.engine.computeAlarms();

		assertThat(alarms).hasSizeGreaterThanOrEqualTo(2);
		assertThat(alarms.get(0).level()).isGreaterThanOrEqualTo(alarms.get(1).level());
	}

	@Test
	void snoozedAlarmIsHiddenFromActive() {
		long now = System.currentTimeMillis();
		stubCurrent(280, now);
		stubLatest(List.of(entry(280, now), entry(275, fiveMinAgo())));

		this.engine.evaluate(); // populates currentAlarms
		assertThat(this.engine.activeAlarms()).isNotEmpty();

		this.engine.snooze("high", 60);
		assertThat(this.engine.activeAlarms()).extracting(AlarmEngine.Alarm::type).doesNotContain("high");
	}

	/* ---------- helpers ---------- */

	private void stubCurrent(int sgv, long dateMs) {
		when(this.entries.findCurrentByType(eq("sgv"))).thenReturn(entry(sgv, dateMs));
	}

	private void stubLatest(List<Entry> latest) {
		when(this.entries.findLatestByType(eq("sgv"), anyInt())).thenReturn(latest);
	}

	private static long fiveMinAgo() {
		return System.currentTimeMillis() - 5L * 60 * 1000;
	}

	private static Entry entry(int sgv, long dateMs) {
		String iso = java.time.Instant.ofEpochMilli(dateMs).toString();
		return new Entry(UUID.randomUUID(), "sgv", dateMs, iso, iso, sgv, "Flat", null, null, null, null, "test", 0,
				java.time.Instant.now());
	}

}
