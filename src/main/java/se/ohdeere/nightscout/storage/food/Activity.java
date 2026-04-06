package se.ohdeere.nightscout.storage.food;

import java.time.Instant;
import java.util.UUID;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("activity")
public record Activity(@Id UUID id, Instant createdAt, String name, Double duration, JsonValue details) {
}
