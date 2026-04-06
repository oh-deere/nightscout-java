package se.ohdeere.nightscout.storage.food;

import java.time.Instant;
import java.util.UUID;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("food")
public record Food(@Id UUID id, Instant createdAt, String name, String category, Double portion, String unit,
		Double carbs, Double fat, Double protein, JsonValue details) {
}
