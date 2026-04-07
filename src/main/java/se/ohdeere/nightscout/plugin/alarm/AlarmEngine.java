package se.ohdeere.nightscout.plugin.alarm;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.NightscoutProperties.Thresholds;
import se.ohdeere.nightscout.plugin.PluginResult;
import se.ohdeere.nightscout.plugin.ar2.Ar2Plugin;
import se.ohdeere.nightscout.service.admin.EffectiveSettings;
import se.ohdeere.nightscout.storage.alarm.AlarmHistoryEntry;
import se.ohdeere.nightscout.storage.alarm.AlarmHistoryRepository;
import se.ohdeere.nightscout.storage.alarm.AlarmSnooze;
import se.ohdeere.nightscout.storage.alarm.AlarmSnoozeRepository;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Multi-alarm engine. Each evaluation cycle inspects current SGV, the 5-minute delta, and
 * the AR2 forecast to produce zero or more alarms.
 *
 * <p>
 * Alarm types currently emitted:
 * <ul>
 * <li>{@code timeago} — stale data (warn / urgent based on age)</li>
 * <li>{@code high} / {@code low} — current SGV outside target/urgent thresholds</li>
 * <li>{@code rise} / {@code fall} — rapid rate of change (warn ≥15 mg/dL/5min, urgent ≥25
 * mg/dL/5min)</li>
 * <li>{@code predicted} — AR2 forecast crosses threshold within 30 minutes</li>
 * </ul>
 */
@Component
public class AlarmEngine {

	private static final Logger LOG = LoggerFactory.getLogger(AlarmEngine.class);

	private final EntryRepository entryRepository;

	private final EffectiveSettings effective;

	private final Ar2Plugin ar2Plugin;

	private final AlarmSnoozeRepository snoozes;

	private final AlarmHistoryRepository history;

	private volatile List<Alarm> currentAlarms = List.of();

	public AlarmEngine(EntryRepository entryRepository, EffectiveSettings effective, Ar2Plugin ar2Plugin,
			AlarmSnoozeRepository snoozes, AlarmHistoryRepository history) {
		this.entryRepository = entryRepository;
		this.effective = effective;
		this.ar2Plugin = ar2Plugin;
		this.snoozes = snoozes;
		this.history = history;
	}

	@Scheduled(fixedRate = 30000, initialDelay = 10000)
	void evaluate() {
		List<Alarm> previous = this.currentAlarms;
		List<Alarm> next = computeAlarms();
		recordTransitions(previous, next);
		this.currentAlarms = next;
	}

	private void recordTransitions(List<Alarm> previous, List<Alarm> next) {
		for (Alarm alarm : next) {
			boolean wasFiringAtSameLevel = previous.stream()
				.anyMatch(p -> p.type().equals(alarm.type()) && p.level() == alarm.level());
			if (!wasFiringAtSameLevel) {
				try {
					this.history.save(new AlarmHistoryEntry(null, Instant.now(), alarm.type(), alarm.level(),
							alarm.title(), alarm.message()));
				}
				catch (Exception ex) {
					LOG.warn("Failed to persist alarm history entry: {}", ex.getMessage());
				}
			}
		}
	}

	/** Hourly cleanup of expired snooze rows. */
	@Scheduled(fixedRate = 3600000, initialDelay = 60000)
	void cleanupSnoozes() {
		int deleted = this.snoozes.deleteExpired(Instant.now());
		if (deleted > 0) {
			LOG.debug("Cleaned up {} expired snooze rows", deleted);
		}
	}

	List<Alarm> computeAlarms() {
		Entry current = this.entryRepository.findCurrentByType("sgv");
		if (current == null || current.sgv() == null) {
			return List.of();
		}

		List<Alarm> alarms = new ArrayList<>();
		int sgv = current.sgv();
		long ageMins = Duration.ofMillis(System.currentTimeMillis() - current.dateMs()).toMinutes();

		// Stale data: short-circuits everything else, since SGV is unreliable.
		if (ageMins > this.effective.alarmTimeagoUrgentMins()) {
			alarms.add(new Alarm(3, "Urgent Stale Data", "No data for " + ageMins + " minutes", "timeago"));
			return alarms;
		}
		if (ageMins > this.effective.alarmTimeagoWarnMins()) {
			alarms.add(new Alarm(2, "Stale Data", "No data for " + ageMins + " minutes", "timeago"));
			return alarms;
		}

		Thresholds t = this.effective.thresholds();

		// Current BG alarms
		if (sgv > t.bgHigh()) {
			alarms.add(new Alarm(3, "Urgent High", formatBg(sgv), "high"));
		}
		else if (sgv < t.bgLow()) {
			alarms.add(new Alarm(3, "Urgent Low", formatBg(sgv), "low"));
		}
		else if (sgv > t.bgTargetTop()) {
			alarms.add(new Alarm(2, "High", formatBg(sgv), "high"));
		}
		else if (sgv < t.bgTargetBottom()) {
			alarms.add(new Alarm(2, "Low", formatBg(sgv), "low"));
		}

		// Delta / rate alarms (need a previous reading within ~10 min)
		int deltaWarn = this.effective.deltaWarnMgdl();
		int deltaUrgent = this.effective.deltaUrgentMgdl();
		List<Entry> recent = this.entryRepository.findLatestByType("sgv", 2);
		if (recent.size() == 2 && recent.get(1).sgv() != null) {
			long dtMins = Duration.ofMillis(recent.get(0).dateMs() - recent.get(1).dateMs()).toMinutes();
			if (dtMins > 0 && dtMins <= 10) {
				int delta = recent.get(0).sgv() - recent.get(1).sgv();
				if (delta >= deltaUrgent) {
					alarms.add(new Alarm(3, "Rapid Rise", "+" + delta + " mg/dL", "rise"));
				}
				else if (delta >= deltaWarn) {
					alarms.add(new Alarm(2, "Rising Fast", "+" + delta + " mg/dL", "rise"));
				}
				else if (delta <= -deltaUrgent) {
					alarms.add(new Alarm(3, "Rapid Fall", delta + " mg/dL", "fall"));
				}
				else if (delta <= -deltaWarn) {
					alarms.add(new Alarm(2, "Falling Fast", delta + " mg/dL", "fall"));
				}
			}
		}

		// Predictive alarm via AR2
		Optional<PluginResult> ar2 = this.ar2Plugin.calculate();
		ar2.ifPresent(result -> {
			Object level = result.data() != null ? result.data().get("level") : null;
			if ("urgent".equals(level)) {
				alarms.add(new Alarm(3, "Predicted Urgent", "AR2 forecast crosses threshold", "predicted"));
			}
			else if ("warn".equals(level)) {
				alarms.add(new Alarm(2, "Predicted", "AR2 forecast trends out of range", "predicted"));
			}
		});

		alarms.sort(Comparator.comparingInt(Alarm::level).reversed());
		return alarms;
	}

	/**
	 * Highest-level non-snoozed alarm. Returns {@code null} when nothing is firing —
	 * preserves the legacy single-alarm API used by {@code /api/v1/properties}.
	 */
	public Alarm currentAlarm() {
		return activeAlarms().stream().findFirst().orElse(null);
	}

	/** All currently active (non-snoozed) alarms, highest level first. */
	public List<Alarm> activeAlarms() {
		List<Alarm> snapshot = this.currentAlarms;
		if (snapshot.isEmpty()) {
			return List.of();
		}
		Map<String, Instant> active = activeSnoozes();
		List<Alarm> visible = new ArrayList<>();
		for (Alarm alarm : snapshot) {
			if (!active.containsKey(alarm.type())) {
				visible.add(alarm);
			}
		}
		return visible;
	}

	public List<Alarm> alarmHistory() {
		return activeAlarms();
	}

	public void snooze(String type, int minutes) {
		snooze(type, minutes, "system");
	}

	public void snooze(String type, int minutes, String actor) {
		Instant now = Instant.now();
		this.snoozes.upsert(type, now.plus(Duration.ofMinutes(minutes)), actor, now);
		LOG.info("Snoozed alarm type '{}' for {} minutes by {}", type, minutes, actor);
	}

	public void clearSnooze(String type) {
		this.snoozes.deleteById(type);
		LOG.info("Cleared snooze for alarm type '{}'", type);
	}

	public Map<String, Instant> activeSnoozes() {
		Map<String, Instant> result = new HashMap<>();
		for (AlarmSnooze snooze : this.snoozes.findActive(Instant.now())) {
			result.put(snooze.type(), snooze.snoozedUntil());
		}
		return result;
	}

	private String formatBg(int mgdl) {
		if ("mmol/l".equalsIgnoreCase(this.effective.units())) {
			return String.valueOf(Math.round(mgdl / 18.0 * 10.0) / 10.0);
		}
		return String.valueOf(mgdl);
	}

	public record Alarm(int level, String title, String message, String type) {
	}

}
