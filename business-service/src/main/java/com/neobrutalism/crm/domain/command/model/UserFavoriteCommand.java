package com.neobrutalism.crm.domain.command.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing user's favorite commands for quick access.
 *
 * @author Admin
 * @version 1.0
 * @since Phase 1
 */
@Entity
@Table(
    name = "user_favorite_commands",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_command", columnNames = {"user_id", "command_id"})
    },
    indexes = {
        @Index(name = "idx_favorite_user", columnList = "user_id, sort_order")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavoriteCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
