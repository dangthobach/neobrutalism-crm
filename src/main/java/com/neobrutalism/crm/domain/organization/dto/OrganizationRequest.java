package com.neobrutalism.crm.domain.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    @Size(max = 200, message = "Name must not exceed 200 characters")
    @Schema(description = "Organization name", example = "Acme Corporation")
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    @Schema(description = "Unique organization code", example = "ACME")
    private String code;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Organization description", example = "Leading provider of innovative solutions")
    private String description;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Contact email", example = "contact@acme.com")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Contact phone", example = "+1234567890")
    private String phone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Schema(description = "Physical address", example = "123 Main St, City, Country")
    private String address;

    @Size(max = 200, message = "Website must not exceed 200 characters")
    @Schema(description = "Website URL", example = "https://www.acme.com")
    private String website;
}
