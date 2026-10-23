package com.smartparking.repository;

import com.smartparking.domain.ParkingFloor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingFloorRepository extends JpaRepository<ParkingFloor, Long> {
    boolean existsByFloorNumber(int floorNumber);
}
