package com.smartparking.strategy;

import com.smartparking.domain.ParkingSpot;
import com.smartparking.domain.VehicleType;
import java.util.Optional;

/** Pluggable spot-selection policy. Implementations must return a spot that is row-locked by the caller's transaction. */
public interface SpotAllocationStrategy {
    Optional<ParkingSpot> allocate(VehicleType vehicleType);
}
