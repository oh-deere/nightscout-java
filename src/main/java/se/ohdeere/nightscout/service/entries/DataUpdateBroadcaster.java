package se.ohdeere.nightscout.service.entries;

import java.util.List;
import java.util.Map;

import se.ohdeere.nightscout.storage.entries.Entry;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Broadcasts new data to WebSocket subscribers when entries are saved.
 */
@Component
public class DataUpdateBroadcaster {

	private final SimpMessagingTemplate messagingTemplate;

	public DataUpdateBroadcaster(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	public void broadcastNewEntries(List<Entry> entries) {
		if (entries.isEmpty()) {
			return;
		}
		List<Map<String, Object>> sgvs = entries.stream()
			.filter(e -> "sgv".equals(e.type()))
			.map(e -> Map.<String, Object>of("_id", e.id().toString(), "type", e.type(), "date", e.dateMs(), "sgv",
					(e.sgv() != null) ? e.sgv() : 0, "direction", (e.direction() != null) ? e.direction() : "",
					"device", (e.device() != null) ? e.device() : ""))
			.toList();

		if (!sgvs.isEmpty()) {
			Object payload = Map.of("sgvs", sgvs);
			this.messagingTemplate.convertAndSend("/topic/dataUpdate", payload);
		}
	}

}
