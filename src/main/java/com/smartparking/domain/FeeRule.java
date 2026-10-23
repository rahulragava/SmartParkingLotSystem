package com.smartparking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Pricing configuration per vehicle type. */
@Entity
@Table(name = "fee_rule")
public class FeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, unique = true, length = 16)
    private VehicleType vehicleType;

    /** Price per started hour. */
    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    /** Maximum charge for any 24h block. */
    @Column(name = "daily_cap", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyCap;

    /** Stays up to this many minutes are free. */
    @Column(name = "grace_minutes", nullable = false)
    private int graceMinutes;

    protected FeeRule() {
    }

    public FeeRule(VehicleType vehicleType, BigDecimal hourlyRate, BigDecimal dailyCap, int graceMinutes) {
        this.vehicleType = vehicleType;
        this.hourlyRate = hourlyRate;
        this.dailyCap = dailyCap;
        this.graceMinutes = graceMinutes;
    }

    public VehicleType getVehicleType() { return vehicleType; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public BigDecimal getDailyCap() { return dailyCap; }
    public int getGraceMinutes() { return graceMinutes; }
}
