package se.ohdeere.nightscout.storage.treatments;

import java.time.Instant;
import java.util.UUID;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("treatments")
public record Treatment(@Id UUID id, String eventType, String createdAtStr, Instant createdAt, String enteredBy,
		String notes, Double insulin, Double carbs, Double glucose, String glucoseType, Double duration, int utcOffset,
		@Column("sync_identifier") String syncIdentifier, @Column("insulin_type") String insulinType,
		JsonValue details) {
}
