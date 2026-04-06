package se.ohdeere.nightscout.storage.profiles;

import java.time.Instant;
import java.util.UUID;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("profiles")
public record Profile(@Id UUID id, Instant createdAt, String defaultProfile, JsonValue store) {
}
