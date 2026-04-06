package se.ohdeere.nightscout.api.v1.entries;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import se.ohdeere.nightscout.storage.entries.Entry;

/**
 * Nightscout-compatible entry DTO. Uses {@code _id} and {@code date} field names to match
 * the upstream API contract.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record EntryDto(@JsonProperty("_id") String id, String type, @JsonProperty("date") long dateMs, String dateString,
		String sysTime, Integer sgv, String direction, Integer noise, Double filtered, Double unfiltered, Integer rssi,
		String device, int utcOffset) {

	static EntryDto from(Entry entry) {
		return new EntryDto(entry.id() != null ? entry.id().toString() : null, entry.type(), entry.dateMs(),
				entry.dateString(), entry.sysTime(), entry.sgv(), entry.direction(), entry.noise(), entry.filtered(),
				entry.unfiltered(), entry.rssi(), entry.device(), entry.utcOffset());
	}

	Entry toEntry() {
		return new Entry(null, this.type, this.dateMs, this.dateString, this.sysTime, this.sgv, this.direction,
				this.noise, this.filtered, this.unfiltered, this.rssi, this.device, this.utcOffset, Instant.now());
	}

}
