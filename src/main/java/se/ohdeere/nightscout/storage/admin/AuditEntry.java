package se.ohdeere.nightscout.storage.admin;

import java.time.Instant;
import java.util.UUID;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("audit_log")
public record AuditEntry(@Id UUID id, @Column("occurred_at") Instant occurredAt,
		@Column("actor_subject") String actorSubject, @Column("actor_kind") String actorKind, String action,
		String target, @Column("before_value") JsonValue beforeValue, @Column("after_value") JsonValue afterValue) {
}
