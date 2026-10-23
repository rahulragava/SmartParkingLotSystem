package com.smartparking.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "parking_floor")
public class ParkingFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor_number", nullable = false, unique = true)
    private int floorNumber;

    protected ParkingFloor() {
    }

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public Long getId() { return id; }
    public int getFloorNumber() { return floorNumber; }
}
