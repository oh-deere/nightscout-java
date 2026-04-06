package se.ohdeere.nightscout.service.bridge.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.remote.librelink.LibreLinkUpClient;
import se.ohdeere.nightscout.remote.librelink.LibreLinkUpClient.AuthTicket;
import se.ohdeere.nightscout.remote.librelink.LibreLinkUpClient.Connection;
import se.ohdeere.nightscout.remote.librelink.LibreLinkUpClient.GlucoseItem;
import se.ohdeere.nightscout.remote.librelink.LibreLinkUpClient.GraphData;
import se.ohdeere.nightscout.remote.librelink.LibreLinkUpProperties;
import se.ohdeere.nightscout.service.entries.EntryService;
import se.ohdeere.nightscout.storage.entries.Entry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "librelink.enabled", havingValue = "true")
class LibreLinkUpBridgeService {

	private static final Logger LOG = LoggerFactory.getLogger(LibreLinkUpBridgeService.class);

	private static final DateTimeFormatter LIBRE_TIMESTAMP = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a",
			Locale.US);

	private final LibreLinkUpClient client;

	private final LibreLinkUpProperties properties;

	private final EntryService entryService;

	private AuthTicket cachedTicket;

	private String cachedPatientId;

	LibreLinkUpBridgeService(LibreLinkUpClient client, LibreLinkUpProperties properties, EntryService entryService) {
		this.client = client;
		this.properties = properties;
		this.entryService = entryService;
	}

	@Scheduled(fixedRateString = "${librelink.poll-interval-ms:300000}", initialDelay = 5000)
	void poll() {
		try {
			ensureAuthenticated();
			if (this.cachedTicket == null) {
				LOG.warn("LibreLink Up: not authenticated, skipping poll");
				return;
			}
			ensurePatientId();
			if (this.cachedPatientId == null) {
				LOG.warn("LibreLink Up: no patient connection found");
				return;
			}

			GraphData data = this.client.getGraph(this.cachedTicket, this.cachedTicket.apiHost(), this.cachedPatientId);

			List<Entry> entries = new ArrayList<>();

			// Add historical graph data points
			for (GlucoseItem item : data.graphData()) {
				entries.add(toEntry(item, null));
			}

			// Add current measurement (has trend arrow)
			if (data.currentMeasurement() != null) {
				entries.add(toEntry(data.currentMeasurement(), data.currentMeasurement().trendArrow()));
			}

			if (!entries.isEmpty()) {
				List<Entry> saved = this.entryService.saveAll(entries);
				if (!saved.isEmpty()) {
					LOG.info("LibreLink Up: stored {} new readings", saved.size());
				}
			}
		}
		catch (Exception ex) {
			LOG.error("LibreLink Up bridge error", ex);
			// Clear ticket on auth errors to force re-login
			if (ex.getMessage() != null && ex.getMessage().contains("401")) {
				this.cachedTicket = null;
			}
		}
	}

	private void ensureAuthenticated() {
		if (this.cachedTicket != null && !this.cachedTicket.isExpired()) {
			return;
		}
		LOG.info("LibreLink Up: logging in as {}", this.properties.username());
		this.cachedTicket = this.client
			.login(this.properties.username(), this.properties.password(), this.properties.apiHost())
			.orElse(null);
		if (this.cachedTicket != null) {
			LOG.info("LibreLink Up: authenticated successfully");
		}
	}

	private void ensurePatientId() {
		if (this.cachedPatientId != null) {
			return;
		}
		String configuredId = this.properties.connectionId();
		if (configuredId != null && !configuredId.isBlank()) {
			this.cachedPatientId = configuredId;
			return;
		}
		List<Connection> connections = this.client.getConnections(this.cachedTicket, this.cachedTicket.apiHost());
		if (!connections.isEmpty()) {
			this.cachedPatientId = connections.getFirst().patientId();
			LOG.info("LibreLink Up: using connection for {}", connections.getFirst().firstName());
		}
	}

	private Entry toEntry(GlucoseItem item, Integer trendArrow) {
		Instant timestamp = parseLibreTimestamp(item.factoryTimestamp());
		long dateMs = timestamp.toEpochMilli();
		String dateString = timestamp.toString();
		String direction = mapTrendArrow(trendArrow);

		return new Entry(null, "sgv", dateMs, dateString, dateString, item.valueInMgPerDl(), direction, null, null,
				null, null, "nightscout-librelink-up", 0, Instant.now());
	}

	static Instant parseLibreTimestamp(String factoryTimestamp) {
		if (factoryTimestamp == null) {
			return Instant.now();
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(factoryTimestamp, LIBRE_TIMESTAMP);
			return ldt.toInstant(ZoneOffset.UTC);
		}
		catch (Exception ex) {
			LOG.debug("Failed to parse LibreLink timestamp: {}", factoryTimestamp);
			return Instant.now();
		}
	}

	static String mapTrendArrow(Integer trendArrow) {
		if (trendArrow == null) {
			return null;
		}
		return switch (trendArrow) {
			case 1 -> "SingleDown";
			case 2 -> "FortyFiveDown";
			case 3 -> "Flat";
			case 4 -> "FortyFiveUp";
			case 5 -> "SingleUp";
			default -> null;
		};
	}

}
