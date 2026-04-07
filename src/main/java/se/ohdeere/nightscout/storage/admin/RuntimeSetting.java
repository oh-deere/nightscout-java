package se.ohdeere.nightscout.storage.admin;

import java.time.Instant;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("runtime_settings")
public record RuntimeSetting(@Id String key, JsonValue value, @Column("updated_at") Instant updatedAt,
		@Column("updated_by") String updatedBy) {
}
