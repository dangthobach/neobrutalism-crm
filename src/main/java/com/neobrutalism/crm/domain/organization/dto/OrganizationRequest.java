package com.neobrutalism.crm.domain.organization.dto;

import com.neobrutalism.crm.common.validation.ValidEmail;
import com.neobrutalism.crm.common.validation.ValidPhone;
import com.neobrutalism.crm.common.validation.ValidUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Organization request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization creation/update request")
public class OrganizationRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    @Schema(description = "Organization name", example = "Acme Corporation", required = true)
    private String name;

    @NotBlank(message = "Code is required")
    @Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must contain only uppercase letters, numbers, dashes, and underscores")
    @Schema(description = "Unique organization code", example = "ACME-001", required = true)
    private String code;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Organization description", example = "Leading provider of innovative solutions")
    private String description;

    @ValidEmail
    @Schema(description = "Contact email", example = "contact@acme.com")
    private String email;

    @ValidPhone
    @Schema(description = "Contact phone", example = "+1-234-567-8900")
    private String phone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Schema(description = "Physical address", example = "123 Main St, City, Country")
    private String address;

    @ValidUrl
    @Schema(description = "Website URL", example = "https://www.acme.com")
    private String website;
}
