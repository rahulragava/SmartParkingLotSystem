package com.smartparking.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateFloorRequest(
        int floorNumber,
        @Min(0) @Max(5000) int smallSpots,
        @Min(0) @Max(5000) int mediumSpots,
        @Min(0) @Max(5000) int largeSpots) {
}
