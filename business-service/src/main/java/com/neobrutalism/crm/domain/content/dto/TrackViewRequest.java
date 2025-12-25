package com.neobrutalism.crm.domain.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Request DTO for tracking content views
 */
@Data
@Schema(description = "Track content view request")
public class TrackViewRequest {

    @Schema(description = "Session ID for tracking anonymous users")
    private String sessionId;

    @Schema(description = "Time spent reading (in seconds)", example = "120")
    @Min(value = 0, message = "Time spent cannot be negative")
    private Integer timeSpentSeconds;

    @Schema(description = "Scroll percentage (0-100)", example = "85")
    @Min(value = 0, message = "Scroll percentage must be between 0 and 100")
    @Max(value = 100, message = "Scroll percentage must be between 0 and 100")
    private Integer scrollPercentage;

    @Schema(description = "Referrer URL")
    private String referrer;
}
