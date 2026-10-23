package com.smartparking.service;

import com.smartparking.domain.SpotType;
import com.smartparking.domain.VehicleType;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.repository.SpotCountProjection;
import com.smartparking.web.dto.AvailabilityResponse;
import com.smartparking.web.dto.AvailabilityResponse.FloorAvailability;
import com.smartparking.web.dto.AvailabilityResponse.TypeAvailability;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Availability is always derived from the spot table (the source of truth), so it can never drift. */
@Service
public class AvailabilityService {

    private final ParkingSpotRepository spotRepository;
    private final Clock clock;

    public AvailabilityService(ParkingSpotRepository spotRepository, Clock clock) {
        this.spotRepository = spotRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse snapshot() {
        List<SpotCountProjection> rows = spotRepository.countByFloorAndType();

        Map<SpotType, long[]> perType = new EnumMap<>(SpotType.class);
        Map<Integer, Map<SpotType, long[]>> perFloor = new TreeMap<>();
        for (SpotCountProjection r : rows) {
            long total = r.getTotal();
            long available = r.getAvailable();
            add(perType, r.getSpotType(), total, available);
            add(perFloor.computeIfAbsent(r.getFloorNumber(), f -> new EnumMap<>(SpotType.class)),
                    r.getSpotType(), total, available);
        }

        List<FloorAvailability> floors = new ArrayList<>();
        perFloor.forEach((floor, types) -> {
            List<TypeAvailability> list = toList(types);
            floors.add(new FloorAvailability(floor,
                    list.stream().mapToLong(TypeAvailability::total).sum(),
                    list.stream().mapToLong(TypeAvailability::available).sum(),
                    list));
        });

        List<TypeAvailability> byType = toList(perType);
        Map<VehicleType, Long> forVehicle = new EnumMap<>(VehicleType.class);
        for (VehicleType vt : VehicleType.values()) {
            long free = 0;
            for (SpotType st : vt.compatibleSpotTypes()) {
                long[] counts = perType.get(st);
                free += counts == null ? 0 : counts[1];
            }
            forVehicle.put(vt, free);
        }

        return new AvailabilityResponse(clock.instant(),
                byType.stream().mapToLong(TypeAvailability::total).sum(),
                byType.stream().mapToLong(TypeAvailability::available).sum(),
                byType, floors, forVehicle);
    }

    private static void add(Map<SpotType, long[]> map, SpotType type, long total, long available) {
        long[] c = map.computeIfAbsent(type, t -> new long[2]);
        c[0] += total;
        c[1] += available;
    }

    private static List<TypeAvailability> toList(Map<SpotType, long[]> map) {
        List<TypeAvailability> out = new ArrayList<>();
        map.forEach((t, c) -> out.add(new TypeAvailability(t, c[0], c[1])));
        return out;
    }
}
