package com.smartparking.web.dto;

import com.smartparking.domain.ParkingTicket;
import com.smartparking.domain.SpotType;
import com.smartparking.domain.TicketStatus;
import com.smartparking.domain.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * For ACTIVE tickets {@code fee} is the running estimate as of now; for COMPLETED tickets it is the final charge.
 */
public record TicketResponse(
        String ticketNumber,
        String licensePlate,
        VehicleType vehicleType,
        String spotNumber,
        SpotType spotType,
        int floorNumber,
        Instant entryTime,
        Instant exitTime,
        long durationMinutes,
        BigDecimal fee,
        TicketStatus status) {

    public static TicketResponse from(ParkingTicket t, long durationMinutes, BigDecimal fee) {
        return new TicketResponse(
                t.getTicketNumber(),
                t.getVehicle().getLicensePlate(),
                t.getVehicleType(),
                t.getSpot().getSpotNumber(),
                t.getSpot().getSpotType(),
                t.getSpot().getFloorNumber(),
                t.getEntryTime(),
                t.getExitTime(),
                durationMinutes,
                fee,
                t.getStatus());
    }
}
