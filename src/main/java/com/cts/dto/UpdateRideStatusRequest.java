package com.cts.dto;

import com.cts.entity.RideStatus;
import lombok.Data;

@Data
public class UpdateRideStatusRequest {
    private RideStatus status;
}