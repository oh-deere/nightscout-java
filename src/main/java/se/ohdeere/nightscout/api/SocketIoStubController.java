package se.ohdeere.nightscout.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal Socket.IO/Engine.IO server for the Nightscout client. Implements the polling
 * transport with namespace connect, authorize event with permissions ack, and dataUpdate
 * events.
 *
 * Engine.IO: 0=open, 2=ping, 3=pong, 4=message Socket.IO (in messages): 0=connect,
 * 2=event, 3=ack
 */
@RestController
class SocketIoStubController {

	private static final Logger LOG = LoggerFactory.getLogger(SocketIoStubController.class);

	private static final String HANDSHAKE = "0{\"sid\":\"stub\",\"upgrades\":[],\"pingInterval\":25000,\"pingTimeout\":60000,\"maxPayload\":1000000}";

	private final LinkedBlockingQueue<String> outQueue = new LinkedBlockingQueue<>();

	private final EntryRepository entryRepository;

	private final ObjectMapper objectMapper;

	SocketIoStubController(EntryRepository entryRepository, ObjectMapper objectMapper) {
		this.entryRepository = entryRepository;
		this.objectMapper = objectMapper;
	}

	@GetMapping({ "/socket.io", "/socket.io/" })
	Object poll(@RequestParam(value = "sid", required = false) String sid) {
		if (sid == null) {
			LOG.debug("Socket.IO GET: handshake");
			this.outQueue.clear();
			return ok(HANDSHAKE);
		}

		try {
			String msg = this.outQueue.poll(25, TimeUnit.SECONDS);
			if (msg != null) {
				LOG.debug("Socket.IO GET: delivering {} chars", msg.length());
				return ok(msg);
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		LOG.debug("Socket.IO GET: timeout, sending ping");
		return ok("2");
	}

	@PostMapping({ "/socket.io", "/socket.io/" })
	ResponseEntity<String> post(HttpServletRequest request) {
		try {
			String body = new String(request.getInputStream().readAllBytes());
			LOG.debug("Socket.IO POST: {}", body.length() > 200 ? body.substring(0, 200) + "..." : body);
			handleClientMessage(body);
		}
		catch (Exception ex) {
			LOG.warn("Socket.IO POST handler error", ex);
		}
		return ResponseEntity.ok("ok");
	}

	private void handleClientMessage(String body) {
		if (body == null || body.isEmpty()) {
			return;
		}

		// Engine.IO ping/pong
		if (body.equals("3") || body.equals("2")) {
			this.outQueue.offer("3");
			return;
		}

		// Socket.IO namespace connect: "40" or "40/alarm,"
		if (body.startsWith("40")) {
			String ns = body.substring(2);
			if (ns.isEmpty()) {
				// Default namespace
				this.outQueue.offer("40{\"sid\":\"main-stub\"}");
				LOG.info("Socket.IO: connected to default namespace");
			}
			else {
				// Custom namespace like /alarm,
				String nsName = ns.endsWith(",") ? ns.substring(0, ns.length() - 1) : ns;
				this.outQueue.offer("40" + nsName + ",{\"sid\":\"" + nsName.replace("/", "") + "-stub\"}");
				LOG.info("Socket.IO: connected to namespace {}", nsName);
			}
			return;
		}

		// Socket.IO event: 42["event",...] or 42N["event",...] (with ack id)
		if (body.startsWith("42")) {
			handleEvent(body.substring(2));
			return;
		}
	}

	private void handleEvent(String payload) {
		// Extract ack ID (digits before the JSON array)
		StringBuilder ackId = new StringBuilder();
		int idx = 0;
		while (idx < payload.length() && Character.isDigit(payload.charAt(idx))) {
			ackId.append(payload.charAt(idx));
			idx++;
		}
		String json = payload.substring(idx);

		if (json.contains("\"authorize\"")) {
			handleAuthorize(ackId.toString());
		}
	}

	private void handleAuthorize(String ackId) {
		// 1. Send ack with full permissions
		Map<String, Object> permissions = Map.of("read", true, "write", true, "write_treatment", true);
		String ackJson = this.objectMapper.writeValueAsString(List.of(permissions));
		String ackMsg = "43" + ackId + ackJson;
		this.outQueue.offer(ackMsg);
		LOG.info("Socket.IO: queued authorize ack ({} chars)", ackMsg.length());

		// 2. Send dataUpdate event with the actual data
		Map<String, Object> data = buildDataPayload();
		String dataJson = this.objectMapper.writeValueAsString(java.util.Arrays.asList("dataUpdate", data));
		String dataMsg = "42" + dataJson;
		this.outQueue.offer(dataMsg);
		LOG.info("Socket.IO: queued dataUpdate ({} chars, {} sgvs)", dataMsg.length(),
				((List<?>) data.get("sgvs")).size());
	}

	private Map<String, Object> buildDataPayload() {
		List<Entry> recent = this.entryRepository.findLatest(288);

		List<Map<String, Object>> sgvs = recent.stream()
			.filter(e -> "sgv".equals(e.type()))
			.map(this::entryToMap)
			.toList();

		// Minimal default profile so the chart can initialize
		Map<String, Object> profileInner = new LinkedHashMap<>();
		profileInner.put("dia", 3);
		profileInner.put("carbs_hr", 20);
		profileInner.put("delay", 20);
		profileInner.put("carbratio", List.of(Map.of("time", "00:00", "value", 10)));
		profileInner.put("sens", List.of(Map.of("time", "00:00", "value", 50)));
		profileInner.put("basal", List.of(Map.of("time", "00:00", "value", 1.0)));
		profileInner.put("target_low", List.of(Map.of("time", "00:00", "value", 70)));
		profileInner.put("target_high", List.of(Map.of("time", "00:00", "value", 180)));
		profileInner.put("units", "mg/dl");
		profileInner.put("timezone", "Europe/Stockholm");

		Map<String, Object> profile = new LinkedHashMap<>();
		profile.put("_id", "default-profile");
		profile.put("defaultProfile", "Default");
		profile.put("startDate", "2020-01-01T00:00:00.000Z");
		profile.put("mills", 1577836800000L);
		profile.put("units", "mg/dl");
		profile.put("store", Map.of("Default", profileInner));
		profile.put("created_at", "2020-01-01T00:00:00.000Z");

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("delta", false);
		data.put("lastUpdated", System.currentTimeMillis());
		data.put("sgvs", sgvs);
		data.put("mbgs", List.of());
		data.put("cals", List.of());
		data.put("treatments", List.of());
		data.put("profiles", List.of(profile));
		data.put("devicestatus", List.of());
		data.put("food", List.of());
		data.put("activity", List.of());
		data.put("sitechangeTreatments", List.of());
		data.put("insulinchangeTreatments", List.of());
		data.put("sensorTreatments", List.of());
		data.put("batteryTreatments", List.of());
		data.put("profileTreatments", List.of());
		data.put("combobolusTreatments", List.of());
		data.put("tempbasalTreatments", List.of());
		return data;
	}

	private Map<String, Object> entryToMap(Entry e) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("_id", e.id().toString());
		map.put("type", e.type());
		map.put("date", e.dateMs());
		map.put("dateString", (e.dateString() != null) ? e.dateString() : "");
		map.put("sgv", (e.sgv() != null) ? e.sgv() : 0);
		map.put("direction", (e.direction() != null) ? e.direction() : "");
		map.put("device", (e.device() != null) ? e.device() : "");
		map.put("noise", (e.noise() != null) ? e.noise() : 0);
		return map;
	}

	private static ResponseEntity<String> ok(String body) {
		return ResponseEntity.ok().header("Content-Type", "text/plain;charset=UTF-8").body(body);
	}

}
