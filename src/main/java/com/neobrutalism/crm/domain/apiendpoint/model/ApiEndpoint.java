package com.neobrutalism.crm.domain.apiendpoint.model;

import com.neobrutalism.crm.common.entity.BaseEntity;
import com.neobrutalism.crm.common.enums.HttpMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_endpoints",
        uniqueConstraints = @UniqueConstraint(columnNames = {"method", "path"}),
        indexes = {
                @Index(name = "idx_api_method_path", columnList = "method, path"),
                @Index(name = "idx_api_tag", columnList = "tag")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpoint extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private HttpMethod method;

    @NotBlank
    @Size(min = 1, max = 500)
    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Size(max = 100)
    @Column(name = "tag", length = 100)
    private String tag;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "requires_auth", nullable = false)
    private Boolean requiresAuth = true;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;
}
