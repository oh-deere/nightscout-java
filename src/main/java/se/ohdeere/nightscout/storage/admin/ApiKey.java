package se.ohdeere.nightscout.storage.admin;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("api_keys")
public record ApiKey(@Id UUID id, String name, @Column("token_hash") String tokenHash, String scope,
		@Column("created_at") Instant createdAt, @Column("created_by") String createdBy,
		@Column("last_used_at") Instant lastUsedAt, @Column("expires_at") Instant expiresAt, boolean enabled) {
}
