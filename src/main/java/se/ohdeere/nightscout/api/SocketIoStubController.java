package se.ohdeere.nightscout.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.NightscoutProperties;
import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal Socket.IO/Engine.IO server for Nightscout client data delivery. Engine.IO
 * packets: 0=open, 2=ping, 3=pong, 4=message. Socket.IO on top: 40=connect, 42=event,
 * 43=ack.
 */
@RestController
class SocketIoStubController {

	private static final Logger LOG = LoggerFactory.getLogger(SocketIoStubController.class);

	private static final String HANDSHAKE = "0{\"sid\":\"stub\",\"upgrades\":[],\"pingInterval\":25000,\"pingTimeout\":60000,\"maxPayload\":1000000}";

	private final LinkedBlockingQueue<String> outQueue = new LinkedBlockingQueue<>();

	private final EntryRepository entryRepository;

	private final ObjectMapper objectMapper;

	private volatile boolean connected = false;

	SocketIoStubController(EntryRepository entryRepository, ObjectMapper objectMapper) {
		this.entryRepository = entryRepository;
		this.objectMapper = objectMapper;
	}

	@GetMapping({ "/socket.io", "/socket.io/" })
	Object poll(@RequestParam(value = "sid", required = false) String sid) {
		if (sid == null) {
			this.connected = false;
			this.outQueue.clear();
			LOG.debug("Socket.IO GET: handshake (new connection)");
			return ok(HANDSHAKE);
		}

		if (!this.connected) {
			this.connected = true;
			LOG.debug("Socket.IO GET: namespace connect");
			// Namespace connect — but also check if there's queued data to batch
			String queued = this.outQueue.poll();
			if (queued != null) {
				// Send both namespace connect and data in one response
				LOG.debug("Socket.IO GET: namespace connect + queued data ({} chars)", queued.length());
				return ok("40{\"sid\":\"ns-stub\"}\u001e" + queued);
			}
			return ok("40{\"sid\":\"ns-stub\"}");
		}

		LOG.debug("Socket.IO GET: long-poll start (queue size={})", this.outQueue.size());
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
		LOG.debug("Socket.IO GET: timeout, returning ping");
		return ok("2");
	}

	@PostMapping({ "/socket.io", "/socket.io/" })
	ResponseEntity<String> post(HttpServletRequest request) {
		try {
			String body = new String(request.getInputStream().readAllBytes());
			LOG.debug("Socket.IO POST: {}", body.length() > 200 ? body.substring(0, 200) : body);

			if (body.contains("authorize")) {
				handleAuthorize(body);
			}
		}
		catch (Exception ex) {
			LOG.debug("Socket.IO POST read error", ex);
		}
		return ResponseEntity.ok("ok");
	}

	public void queueDataUpdate(List<Entry> entries) {
		Map<String, Object> data = buildDataPayload(entries);
		String json = this.objectMapper.writeValueAsString(List.of("dataUpdate", data));
		this.outQueue.offer("42" + json);
	}

	private void handleAuthorize(String body) {
		String payload = body;
		// Strip Engine.IO message prefix if present
		if (payload.startsWith("4")) {
			payload = payload.substring(2);
		}

		// Extract ack ID (digits before the JSON array)
		StringBuilder ackId = new StringBuilder();
		int idx = 0;
		while (idx < payload.length() && Character.isDigit(payload.charAt(idx))) {
			ackId.append(payload.charAt(idx));
			idx++;
		}

		Map<String, Object> data = buildDataPayload(null);
		LOG.info("Socket.IO: authorize response with {} sgvs (ackId={})", ((List<?>) data.get("sgvs")).size(), ackId);

		if (ackId.length() > 0) {
			String response = this.objectMapper.writeValueAsString(java.util.Arrays.asList(null, data));
			String msg = "43" + ackId + response;
			boolean ok = this.outQueue.offer(msg);
			LOG.info("Socket.IO: queued ack ({} chars, ok={}, queueSize={})", msg.length(), ok, this.outQueue.size());
		}
		else {
			String response = this.objectMapper.writeValueAsString(List.of("dataUpdate", data));
			boolean ok = this.outQueue.offer("42" + response);
			LOG.info("Socket.IO: queued event ({} chars, ok={}, queueSize={})", response.length(), ok,
					this.outQueue.size());
		}
	}

	private Map<String, Object> buildDataPayload(List<Entry> newEntries) {
		List<Entry> recent = (newEntries != null && !newEntries.isEmpty()) ? newEntries
				: this.entryRepository.findLatest(288);

		List<Map<String, Object>> sgvs = recent.stream()
			.filter(e -> "sgv".equals(e.type()))
			.map(this::entryToMap)
			.toList();

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("sgvs", sgvs);
		data.put("mbgs", List.of());
		data.put("cals", List.of());
		data.put("treatments", List.of());
		data.put("profiles", List.of());
		data.put("devicestatus", List.of());
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
