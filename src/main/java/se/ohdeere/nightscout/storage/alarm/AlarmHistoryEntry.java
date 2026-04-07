package se.ohdeere.nightscout.storage.alarm;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("alarm_history")
public record AlarmHistoryEntry(@Id UUID id, @Column("occurred_at") Instant occurredAt, String type, int level,
		String title, String message) {
}
