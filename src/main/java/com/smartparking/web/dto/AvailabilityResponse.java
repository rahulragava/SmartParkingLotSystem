package com.smartparking.web.dto;

import com.smartparking.domain.SpotType;
import com.smartparking.domain.VehicleType;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AvailabilityResponse(
        Instant asOf,
        long totalSpots,
        long availableSpots,
        List<TypeAvailability> byType,
        List<FloorAvailability> byFloor,
        Map<VehicleType, Long> availableForVehicleType) {

    public record TypeAvailability(SpotType spotType, long total, long available) {
    }

    public record FloorAvailability(int floorNumber, long total, long available, List<TypeAvailability> byType) {
    }
}
