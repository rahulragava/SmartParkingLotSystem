package com.smartparking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One parking session. {@code activeVehicleId} is set to the vehicle id while the ticket is ACTIVE
 * and cleared on completion; its unique constraint guarantees at most one active session per
 * vehicle even under concurrent check-ins (portable alternative to a partial unique index).
 */
@Entity
@Table(name = "parking_ticket", indexes = {
        @Index(name = "idx_ticket_status", columnList = "status")
})
public class ParkingTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 40)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot spot;

    /** Vehicle type at entry time (snapshot; billing must not change if the vehicle record does). */
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 16)
    private VehicleType vehicleType;

    @Column(name = "entry_time", nullable = false)
    private Instant entryTime;

    @Column(name = "exit_time")
    private Instant exitTime;

    @Column(precision = 12, scale = 2)
    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketStatus status;

    @Column(name = "active_vehicle_id", unique = true)
    private Long activeVehicleId;

    protected ParkingTicket() {
    }

    public ParkingTicket(String ticketNumber, Vehicle vehicle, ParkingSpot spot, Instant entryTime) {
        this.ticketNumber = ticketNumber;
        this.vehicle = vehicle;
        this.spot = spot;
        this.vehicleType = vehicle.getVehicleType();
        this.entryTime = entryTime;
        this.status = TicketStatus.ACTIVE;
        this.activeVehicleId = vehicle.getId();
    }

    public void complete(Instant exitTime, BigDecimal fee) {
        this.exitTime = exitTime;
        this.fee = fee;
        this.status = TicketStatus.COMPLETED;
        this.activeVehicleId = null;
    }

    public Long getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public Vehicle getVehicle() { return vehicle; }
    public ParkingSpot getSpot() { return spot; }
    public VehicleType getVehicleType() { return vehicleType; }
    public Instant getEntryTime() { return entryTime; }
    public Instant getExitTime() { return exitTime; }
    public BigDecimal getFee() { return fee; }
    public TicketStatus getStatus() { return status; }
}
