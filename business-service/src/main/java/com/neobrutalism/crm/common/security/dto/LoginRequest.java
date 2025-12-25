package com.neobrutalism.crm.common.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login credentials")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username or email", example = "admin", required = true)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "password123", required = true)
    private String password;

    @Schema(description = "Tenant ID (optional, defaults to user's tenant)", example = "default")
    private String tenantId;
    
    @Schema(description = "Remember me option", example = "false")
    private Boolean rememberMe = false;
}
