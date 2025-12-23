package com.neobrutalism.crm.domain.command.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity tracking user command execution history.
 *
 * Used for:
 * - Recent commands list
 * - Personalized command suggestions
 * - Usage analytics
 *
 * @author Admin
 * @version 1.0
 * @since Phase 1
 */
@Entity
@Table(
    name = "user_command_history",
    indexes = {
        @Index(name = "idx_command_history_user", columnList = "user_id, executed_at DESC"),
        @Index(name = "idx_command_history_tenant_user", columnList = "tenant_id, user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCommandHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "execution_time_ms", nullable = false)
    private Long executionTimeMs;

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData; // JSON context (e.g., page where command was executed)

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = Instant.now();
        }
    }
}
