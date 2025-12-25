package com.neobrutalism.crm.domain.screenapi.model;

import com.neobrutalism.crm.common.entity.BaseEntity;
import com.neobrutalism.crm.common.enums.PermissionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "screen_api_endpoints",
        uniqueConstraints = @UniqueConstraint(columnNames = {"screen_id", "endpoint_id"}),
        indexes = {
                @Index(name = "idx_sae_screen", columnList = "screen_id"),
                @Index(name = "idx_sae_endpoint", columnList = "endpoint_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScreenApiEndpoint extends BaseEntity {

    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @Column(name = "endpoint_id", nullable = false)
    private UUID endpointId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "required_permission", nullable = false, length = 20)
    private PermissionType requiredPermission = PermissionType.READ;
}
