package com.cts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for assigning a driver to a ride")
public class AssignDriverRequest {

    @NotNull(message = "Driver ID is required")
    @Positive(message = "Driver ID must be a positive number")
    @Schema(description = "ID of the driver to assign", example = "2")
    private Long driverId;
}
