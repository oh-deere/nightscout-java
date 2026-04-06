package se.ohdeere.nightscout.storage.entries;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("entries")
public record Entry(@Id UUID id, String type, long dateMs, String dateString, String sysTime, Integer sgv,
		String direction, Integer noise, Double filtered, Double unfiltered, Integer rssi, String device, int utcOffset,
		Instant createdAt) {
}
