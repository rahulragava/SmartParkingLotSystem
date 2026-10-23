package com.smartparking.web.dto;

import com.smartparking.domain.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CheckInRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9 -]{2,20}$", message = "must be 2-20 letters, digits, spaces or hyphens")
        String licensePlate,
        @NotNull VehicleType vehicleType) {
}
