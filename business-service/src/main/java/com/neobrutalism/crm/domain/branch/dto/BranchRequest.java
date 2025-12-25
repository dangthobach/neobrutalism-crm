package com.neobrutalism.crm.domain.branch.dto;

import com.neobrutalism.crm.domain.branch.Branch;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for Branch operations
 */
@Data
@Schema(description = "Branch request data")
public class BranchRequest {

    @NotBlank(message = "Branch code is required")
    @Size(max = 50, message = "Branch code must be at most 50 characters")
    @Schema(description = "Unique branch code (e.g., HN-001)", example = "HN-001")
    private String code;

    @NotBlank(message = "Branch name is required")
    @Size(max = 200, message = "Branch name must be at most 200 characters")
    @Schema(description = "Branch name", example = "Hanoi Branch")
    private String name;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Schema(description = "Branch description", example = "Main branch in Hanoi")
    private String description;

    @Schema(description = "Organization ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID organizationId;

    @Schema(description = "Parent branch ID (null for root branches)", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID parentId;

    @Schema(description = "Branch type", example = "LOCAL", allowableValues = {"HQ", "REGIONAL", "LOCAL"})
    private Branch.BranchType branchType;

    @Schema(description = "Manager user ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID managerId;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    @Schema(description = "Contact email", example = "hanoi@company.com")
    private String email;

    @Size(max = 50, message = "Phone must be at most 50 characters")
    @Schema(description = "Contact phone", example = "+84-24-1234-5678")
    private String phone;

    @Size(max = 500, message = "Address must be at most 500 characters")
    @Schema(description = "Branch address", example = "123 Nguyen Trai St, Hanoi, Vietnam")
    private String address;

    @Schema(description = "Display order", example = "0")
    private Integer displayOrder;
}
