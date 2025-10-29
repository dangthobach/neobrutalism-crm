package com.neobrutalism.crm.domain.user.dto;

import com.neobrutalism.crm.common.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile update request")
public class UserProfileUpdateRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @Schema(description = "User's first name", example = "John", required = true)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Schema(description = "User's last name", example = "Doe", required = true)
    private String lastName;

    @ValidPhone
    @Schema(description = "Phone number", example = "+1234567890")
    private String phone;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @Schema(description = "Avatar image URL", example = "https://example.com/avatar.jpg")
    private String avatar;
}
