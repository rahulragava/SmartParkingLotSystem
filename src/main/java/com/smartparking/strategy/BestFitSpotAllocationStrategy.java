package com.smartparking.strategy;

import com.smartparking.domain.ParkingSpot;
import com.smartparking.domain.SpotType;
import com.smartparking.domain.VehicleType;
import com.smartparking.repository.ParkingSpotRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Best-fit allocation: try the smallest compatible spot type first (so buses/large spots are kept free
 * for vehicles that need them), and within a type pick the lowest floor / spot number (closest to the
 * entrance). Candidate rows are locked with SKIP LOCKED, so parallel requests get distinct spots.
 *
 * <p>If no unlocked spot is found but free spots still exist (they are merely locked by in-flight
 * check-ins), we retry briefly before concluding the lot is full.
 */
@Component
public class BestFitSpotAllocationStrategy implements SpotAllocationStrategy {

    static final int MAX_ATTEMPTS = 5;
    static final long BACKOFF_MILLIS = 25;

    private final ParkingSpotRepository spotRepository;

    public BestFitSpotAllocationStrategy(ParkingSpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    @Override
    public Optional<ParkingSpot> allocate(VehicleType vehicleType) {
        List<SpotType> candidates = vehicleType.compatibleSpotTypes();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            for (SpotType type : candidates) {
                List<ParkingSpot> found = spotRepository.findFreeForUpdate(type, PageRequest.of(0, 1));
                if (!found.isEmpty()) {
                    return Optional.of(found.get(0));
                }
            }
            if (spotRepository.countFree(candidates) == 0) {
                return Optional.empty();
            }
            pause();
        }
        return Optional.empty();
    }

    private void pause() {
        try {
            Thread.sleep(BACKOFF_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
