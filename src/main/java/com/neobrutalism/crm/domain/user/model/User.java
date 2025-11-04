package com.neobrutalism.crm.domain.user.model;

import com.neobrutalism.crm.common.entity.TenantAwareAggregateRoot;
import com.neobrutalism.crm.common.validation.ValidEmail;
import com.neobrutalism.crm.common.validation.ValidPhone;
import com.neobrutalism.crm.domain.user.event.UserCreatedEvent;
import com.neobrutalism.crm.domain.user.event.UserStatusChangedEvent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * User entity
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_org_id", columnList = "organization_id"),
        @Index(name = "idx_user_deleted_id", columnList = "deleted, id"),
        @Index(name = "idx_user_status", columnList = "status"),
        // ✅ PHASE 1: Performance optimization indexes
        @Index(name = "idx_user_tenant", columnList = "tenant_id"),
        @Index(name = "idx_user_tenant_org_deleted", columnList = "tenant_id, organization_id, deleted"),
        @Index(name = "idx_user_tenant_status_deleted", columnList = "tenant_id, status, deleted"),
        @Index(name = "idx_user_branch", columnList = "branch_id"),
        @Index(name = "idx_user_last_login", columnList = "last_login_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends TenantAwareAggregateRoot<UserStatus> {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @ValidEmail
    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @NotBlank(message = "Password hash is required")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @ValidPhone
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /**
     * Branch ID - Chi nhánh mà user thuộc về
     * Dùng để filter data theo branch
     */
    @Column(name = "branch_id")
    private UUID branchId;

    /**
     * Data Scope - Phạm vi dữ liệu user có thể truy cập
     * ALL_BRANCHES: Xem tất cả branches (Management role)
     * CURRENT_BRANCH: Chỉ xem branch hiện tại (ORC role)
     * SELF_ONLY: Chỉ xem bản ghi của mình (Maker/Checker role)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_scope", nullable = false, length = 20)
    private DataScope dataScope = DataScope.SELF_ONLY;

    /**
     * Member tier for LMS/CMS access control
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "member_tier", nullable = false, length = 20)
    private com.neobrutalism.crm.common.enums.MemberTier memberTier = com.neobrutalism.crm.common.enums.MemberTier.FREE;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Override
    protected Set<UserStatus> getAllowedTransitions(UserStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING -> Set.of(UserStatus.ACTIVE, UserStatus.INACTIVE);
            case ACTIVE -> Set.of(UserStatus.SUSPENDED, UserStatus.LOCKED, UserStatus.INACTIVE);
            case SUSPENDED -> Set.of(UserStatus.ACTIVE, UserStatus.INACTIVE);
            case LOCKED -> Set.of(UserStatus.ACTIVE, UserStatus.INACTIVE);
            case INACTIVE -> Set.of(UserStatus.ACTIVE);
        };
    }

    @Override
    protected UserStatus getInitialStatus() {
        return UserStatus.PENDING;
    }

    @Override
    protected void onStatusChanged(UserStatus oldStatus, UserStatus newStatus) {
        super.onStatusChanged(oldStatus, newStatus);
        registerEvent(new UserStatusChangedEvent(
                this.getId().toString(),
                oldStatus,
                newStatus,
                this.getStatusChangedBy()
        ));
    }

    @PostPersist
    protected void onCreated() {
        registerEvent(new UserCreatedEvent(
                this.getId().toString(),
                this.getUsername(),
                this.getEmail(),
                this.getCreatedBy()
        ));
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if account is locked
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    /**
     * Record failed login attempt
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plusSeconds(30 * 60); // Lock for 30 minutes
            transitionTo(UserStatus.LOCKED, "system", "Too many failed login attempts");
        }
    }

    /**
     * Reset failed login attempts
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Record successful login
     */
    public void recordSuccessfulLogin(String ipAddress) {
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = ipAddress;
        resetFailedLoginAttempts();
    }
}
