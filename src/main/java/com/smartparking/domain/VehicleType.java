package com.smartparking.domain;

import java.util.List;

public enum VehicleType {
    MOTORCYCLE(List.of(SpotType.SMALL, SpotType.MEDIUM, SpotType.LARGE)),
    CAR(List.of(SpotType.MEDIUM, SpotType.LARGE)),
    BUS(List.of(SpotType.LARGE));

    private final List<SpotType> compatibleSpotTypes;

    VehicleType(List<SpotType> compatibleSpotTypes) {
        this.compatibleSpotTypes = compatibleSpotTypes;
    }

    /** Spot types this vehicle fits in, ordered best-fit (smallest) first. */
    public List<SpotType> compatibleSpotTypes() {
        return compatibleSpotTypes;
    }
}
