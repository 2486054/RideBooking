package com.cts.dto;

import com.cts.entity.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideResponse {
    private Long rideId;
    private Long userId;
    private Long driverId;
    private String pickupLocation;
    private String dropoffLocation;
    private BigDecimal fare;
    private RideStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
