package com.cts.dto;

import com.cts.entity.RideStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response body containing ride details")
public class RideResponse {

    @Schema(description = "Ride ID", example = "1")
    private Long rideId;

    @Schema(description = "Customer's user ID", example = "1")
    private Long userId;

    @Schema(description = "Assigned driver's ID", example = "2")
    private Long driverId;

    @Schema(description = "Pickup location", example = "MG Road, Chennai")
    private String pickupLocation;

    @Schema(description = "Dropoff location", example = "T Nagar, Chennai")
    private String dropoffLocation;

    @Schema(description = "Calculated fare", example = "254.00")
    private BigDecimal fare;

    @Schema(description = "Current ride status", example = "REQUESTED")
    private RideStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
