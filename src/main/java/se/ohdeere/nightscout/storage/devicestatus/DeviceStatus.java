package se.ohdeere.nightscout.storage.devicestatus;

import java.time.Instant;
import java.util.UUID;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("device_status")
public record DeviceStatus(@Id UUID id, Instant createdAt, String device, JsonValue uploader, JsonValue pump,
		JsonValue openaps, JsonValue loop, JsonValue xdripjs, JsonValue raw) {
}
