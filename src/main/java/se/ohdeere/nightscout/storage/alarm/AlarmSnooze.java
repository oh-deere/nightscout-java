package se.ohdeere.nightscout.storage.alarm;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("alarm_snoozes")
public record AlarmSnooze(@Id String type, @Column("snoozed_until") Instant snoozedUntil,
		@Column("snoozed_by") String snoozedBy, @Column("snoozed_at") Instant snoozedAt) {
}
