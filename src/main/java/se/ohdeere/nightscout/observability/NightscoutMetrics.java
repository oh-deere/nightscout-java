package se.ohdeere.nightscout.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import se.ohdeere.nightscout.plugin.alarm.AlarmEngine;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;
import se.ohdeere.nightscout.storage.treatments.TreatmentRepository;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Custom Micrometer meters surfaced via {@code /actuator/prometheus}. Gauges read live
 * from the repositories on each scrape; counters are mutated by the rest of the system
 * via {@link #recordEntryWritten}, {@link #recordTreatmentWritten}, and
 * {@link #recordBridgePoll}.
 *
 * <p>
 * Naming follows Prometheus conventions: lowercase with units as the suffix where
 * appropriate.
 */
@Component
public class NightscoutMetrics {

	private final MeterRegistry registry;

	private final EntryRepository entries;

	private final TreatmentRepository treatments;

	private final AlarmEngine alarms;

	private Counter entriesWritten;

	private Counter treatmentsWritten;

	private Counter bridgePolls;

	private Counter bridgeFailures;

	public NightscoutMetrics(MeterRegistry registry, EntryRepository entries, TreatmentRepository treatments,
			AlarmEngine alarms) {
		this.registry = registry;
		this.entries = entries;
		this.treatments = treatments;
		this.alarms = alarms;
	}

	@PostConstruct
	void register() {
		this.entriesWritten = Counter.builder("nightscout_entries_written_total")
			.description("Total entries written since process start")
			.register(this.registry);

		this.treatmentsWritten = Counter.builder("nightscout_treatments_written_total")
			.description("Total treatments written since process start")
			.register(this.registry);

		this.bridgePolls = Counter.builder("nightscout_bridge_polls_total")
			.description("LibreLink Up bridge polls (successful)")
			.register(this.registry);

		this.bridgeFailures = Counter.builder("nightscout_bridge_failures_total")
			.description("LibreLink Up bridge polls that failed")
			.register(this.registry);

		// Live gauges — these recompute on every scrape.
		this.registry.gauge("nightscout_entries_total", this.entries, repo -> (double) repo.count());
		this.registry.gauge("nightscout_treatments_total", this.treatments, repo -> (double) repo.count());
		this.registry.gauge("nightscout_current_sgv_mgdl", this.entries, this::currentSgv);
		this.registry.gauge("nightscout_current_sgv_age_seconds", this.entries, this::currentSgvAge);
		this.registry.gauge("nightscout_active_alarms", this.alarms, engine -> (double) engine.activeAlarms().size());
	}

	public void recordEntryWritten() {
		this.entriesWritten.increment();
	}

	public void recordEntryWritten(int count) {
		this.entriesWritten.increment(count);
	}

	public void recordTreatmentWritten(int count) {
		this.treatmentsWritten.increment(count);
	}

	public void recordBridgePoll() {
		this.bridgePolls.increment();
	}

	public void recordBridgeFailure() {
		this.bridgeFailures.increment();
	}

	private double currentSgv(EntryRepository repo) {
		Entry current = repo.findCurrentByType("sgv");
		return (current != null && current.sgv() != null) ? current.sgv() : Double.NaN;
	}

	private double currentSgvAge(EntryRepository repo) {
		Entry current = repo.findCurrentByType("sgv");
		if (current == null) {
			return Double.NaN;
		}
		return (System.currentTimeMillis() - current.dateMs()) / 1000.0;
	}

}
