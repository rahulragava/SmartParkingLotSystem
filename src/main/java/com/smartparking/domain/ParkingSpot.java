package com.smartparking.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "parking_spot", indexes = {
        @Index(name = "idx_spot_type_occupied", columnList = "spot_type, occupied")
})
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_number", nullable = false, unique = true, length = 32)
    private String spotNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 16)
    private SpotType spotType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private ParkingFloor floor;

    /** Denormalized from the floor so allocation queries can order without a join (and lock only spot rows). */
    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(nullable = false)
    private boolean occupied;

    protected ParkingSpot() {
    }

    public ParkingSpot(String spotNumber, SpotType spotType, ParkingFloor floor) {
        this.spotNumber = spotNumber;
        this.spotType = spotType;
        this.floor = floor;
        this.floorNumber = floor.getFloorNumber();
    }

    public void occupy() {
        if (occupied) {
            throw new IllegalStateException("Spot " + spotNumber + " is already occupied");
        }
        this.occupied = true;
    }

    public void release() {
        this.occupied = false;
    }

    public Long getId() { return id; }
    public String getSpotNumber() { return spotNumber; }
    public SpotType getSpotType() { return spotType; }
    public ParkingFloor getFloor() { return floor; }
    public int getFloorNumber() { return floorNumber; }
    public boolean isOccupied() { return occupied; }
}
