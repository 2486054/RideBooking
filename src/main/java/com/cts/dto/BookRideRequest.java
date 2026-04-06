package com.cts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for booking a ride")
public class BookRideRequest {

    @NotBlank(message = "Pickup location is required")
    @Size(min = 3, max = 500, message = "Pickup location must be between 3 and 500 characters")
    @Schema(description = "Pickup address", example = "MG Road, Chennai")
    private String pickupLocation;

    @NotBlank(message = "Dropoff location is required")
    @Size(min = 3, max = 500, message = "Dropoff location must be between 3 and 500 characters")
    @Schema(description = "Dropoff address", example = "T Nagar, Chennai")
    private String dropoffLocation;
}

