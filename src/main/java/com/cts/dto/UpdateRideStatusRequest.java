package com.cts.dto;

import com.cts.entity.RideStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for updating ride status")
public class UpdateRideStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New ride status", example = "IN_PROGRESS")
    private RideStatus status;
}
