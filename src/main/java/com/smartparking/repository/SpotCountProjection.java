package com.smartparking.repository;

import com.smartparking.domain.SpotType;

public interface SpotCountProjection {
    Integer getFloorNumber();
    SpotType getSpotType();
    Long getTotal();
    Long getAvailable();
}
