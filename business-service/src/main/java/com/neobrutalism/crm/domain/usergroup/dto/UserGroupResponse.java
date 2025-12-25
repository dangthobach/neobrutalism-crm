package com.neobrutalism.crm.domain.usergroup.dto;

import com.neobrutalism.crm.domain.usergroup.model.UserGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User group assignment response")
public class UserGroupResponse {

    @Schema(description = "User group ID")
    private UUID id;

    @Schema(description = "User ID")
    private UUID userId;

    @Schema(description = "Group ID")
    private UUID groupId;

    @Schema(description = "Is primary group")
    private Boolean isPrimary;

    @Schema(description = "Joined timestamp")
    private Instant joinedAt;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    public static UserGroupResponse from(UserGroup userGroup) {
        return UserGroupResponse.builder()
                .id(userGroup.getId())
                .userId(userGroup.getUserId())
                .groupId(userGroup.getGroupId())
                .isPrimary(userGroup.getIsPrimary())
                .joinedAt(userGroup.getJoinedAt())
                .createdAt(userGroup.getCreatedAt())
                .createdBy(userGroup.getCreatedBy())
                .build();
    }
}
