package se.ohdeere.nightscout.plugin.alarm;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.NightscoutProperties;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlarmEngine {

	private static final Logger LOG = LoggerFactory.getLogger(AlarmEngine.class);

	private final EntryRepository entryRepository;

	private final NightscoutProperties properties;

	private final Map<String, Instant> snoozedAlarms = new ConcurrentHashMap<>();

	private volatile Alarm currentAlarm;

	public AlarmEngine(EntryRepository entryRepository, NightscoutProperties properties) {
		this.entryRepository = entryRepository;
		this.properties = properties;
	}

	@Scheduled(fixedRate = 30000, initialDelay = 10000)
	void evaluate() {
		Entry current = this.entryRepository.findCurrentByType("sgv");
		if (current == null || current.sgv() == null) {
			this.currentAlarm = null;
			return;
		}

		int sgv = current.sgv();
		long ageMins = Duration.ofMillis(System.currentTimeMillis() - current.dateMs()).toMinutes();

		// Stale data alarm
		if (ageMins > this.properties.alarmTimeagoUrgentMins()) {
			setAlarm(new Alarm(3, "Urgent Stale Data", "No data for " + ageMins + " minutes", "timeago"));
			return;
		}
		if (ageMins > this.properties.alarmTimeagoWarnMins()) {
			setAlarm(new Alarm(2, "Stale Data", "No data for " + ageMins + " minutes", "timeago"));
			return;
		}

		// BG alarms
		if (sgv > this.properties.thresholds().bgHigh()) {
			setAlarm(new Alarm(3, "Urgent High", formatBg(sgv), "high"));
		}
		else if (sgv < this.properties.thresholds().bgLow()) {
			setAlarm(new Alarm(3, "Urgent Low", formatBg(sgv), "low"));
		}
		else if (sgv > this.properties.thresholds().bgTargetTop()) {
			setAlarm(new Alarm(2, "High", formatBg(sgv), "high"));
		}
		else if (sgv < this.properties.thresholds().bgTargetBottom()) {
			setAlarm(new Alarm(2, "Low", formatBg(sgv), "low"));
		}
		else {
			this.currentAlarm = null;
		}
	}

	public Alarm currentAlarm() {
		if (this.currentAlarm != null && isSnoozed(this.currentAlarm.type())) {
			return null;
		}
		return this.currentAlarm;
	}

	public List<Alarm> alarmHistory() {
		Alarm current = currentAlarm();
		return (current != null) ? List.of(current) : List.of();
	}

	public void snooze(String type, int minutes) {
		this.snoozedAlarms.put(type, Instant.now().plus(Duration.ofMinutes(minutes)));
		LOG.info("Snoozed alarm type '{}' for {} minutes", type, minutes);
	}

	private boolean isSnoozed(String type) {
		Instant until = this.snoozedAlarms.get(type);
		if (until != null && Instant.now().isBefore(until)) {
			return true;
		}
		this.snoozedAlarms.remove(type);
		return false;
	}

	private void setAlarm(Alarm alarm) {
		if (this.currentAlarm == null || this.currentAlarm.level() != alarm.level()
				|| !this.currentAlarm.type().equals(alarm.type())) {
			LOG.debug("Alarm: level={} title={}", alarm.level(), alarm.title());
		}
		this.currentAlarm = alarm;
	}

	private String formatBg(int mgdl) {
		double display = this.properties.toDisplayUnits(mgdl);
		if ("mmol/l".equalsIgnoreCase(this.properties.units())) {
			return String.valueOf(display);
		}
		return String.valueOf((int) display);
	}

	public record Alarm(int level, String title, String message, String type) {
	}

}
