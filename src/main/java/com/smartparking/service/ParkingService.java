package com.smartparking.service;

import com.smartparking.domain.*;
import com.smartparking.event.AvailabilityChangedEvent;
import com.smartparking.repository.FeeRuleRepository;
import com.smartparking.repository.ParkingTicketRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.strategy.SpotAllocationStrategy;
import com.smartparking.web.dto.TicketResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingService {

    private final VehicleRepository vehicleRepository;
    private final ParkingTicketRepository ticketRepository;
    private final FeeRuleRepository feeRuleRepository;
    private final SpotAllocationStrategy allocationStrategy;
    private final FeeCalculator feeCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ParkingService(VehicleRepository vehicleRepository,
                          ParkingTicketRepository ticketRepository,
                          FeeRuleRepository feeRuleRepository,
                          SpotAllocationStrategy allocationStrategy,
                          FeeCalculator feeCalculator,
                          ApplicationEventPublisher eventPublisher,
                          Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.ticketRepository = ticketRepository;
        this.feeRuleRepository = feeRuleRepository;
        this.allocationStrategy = allocationStrategy;
        this.feeCalculator = feeCalculator;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Allocates a spot and opens a ticket. Everything happens in one transaction: if any step fails
     * (lot full, duplicate active session, constraint violation) the spot lock and changes roll back.
     */
    @Transactional
    public TicketResponse checkIn(String rawPlate, VehicleType vehicleType) {
        String plate = normalizePlate(rawPlate);

        Vehicle vehicle = vehicleRepository.findByLicensePlate(plate)
                .orElseGet(() -> vehicleRepository.save(new Vehicle(plate, vehicleType)));

        if (ticketRepository.existsByVehicleAndStatus(vehicle, TicketStatus.ACTIVE)) {
            throw ParkingException.alreadyParked(plate);
        }
        vehicle.setVehicleType(vehicleType);

        ParkingSpot spot = allocationStrategy.allocate(vehicleType)
                .orElseThrow(() -> ParkingException.lotFull(vehicleType.name()));
        spot.occupy();

        Instant now = clock.instant();
        ParkingTicket ticket = ticketRepository.save(new ParkingTicket(newTicketNumber(), vehicle, spot, now));

        eventPublisher.publishEvent(new AvailabilityChangedEvent());
        return TicketResponse.from(ticket, 0, BigDecimal.ZERO.setScale(2));
    }

    /** Closes the ticket, computes the fee and frees the spot. Safe against duplicate/simultaneous exits. */
    @Transactional
    public TicketResponse checkOut(String ticketNumber) {
        ParkingTicket ticket = ticketRepository.findByTicketNumberForUpdate(ticketNumber)
                .orElseThrow(() -> ParkingException.ticketNotFound(ticketNumber));
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw ParkingException.ticketNotActive(ticketNumber);
        }

        Instant now = clock.instant();
        BigDecimal fee = feeCalculator.calculate(ruleFor(ticket.getVehicleType()), ticket.getEntryTime(), now);
        ticket.complete(now, fee);
        ticket.getSpot().release();

        eventPublisher.publishEvent(new AvailabilityChangedEvent());
        return TicketResponse.from(ticket, FeeCalculator.billableMinutes(ticket.getEntryTime(), now), fee);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(String ticketNumber) {
        ParkingTicket ticket = ticketRepository.findDetailedByTicketNumber(ticketNumber)
                .orElseThrow(() -> ParkingException.ticketNotFound(ticketNumber));
        if (ticket.getStatus() == TicketStatus.COMPLETED) {
            return TicketResponse.from(ticket,
                    FeeCalculator.billableMinutes(ticket.getEntryTime(), ticket.getExitTime()), ticket.getFee());
        }
        Instant now = clock.instant();
        BigDecimal estimate = feeCalculator.calculate(ruleFor(ticket.getVehicleType()), ticket.getEntryTime(), now);
        return TicketResponse.from(ticket, FeeCalculator.billableMinutes(ticket.getEntryTime(), now), estimate);
    }

    private FeeRule ruleFor(VehicleType type) {
        return feeRuleRepository.findByVehicleType(type)
                .orElseThrow(() -> new IllegalStateException("No fee rule configured for " + type));
    }

    static String normalizePlate(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "");
    }

    private static String newTicketNumber() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
