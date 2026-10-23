package com.smartparking.service;

import com.smartparking.repository.*;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Shared Spring context + clean tables per test. Not @Transactional on purpose: concurrency tests need real commits. */
@SpringBootTest
@Import(TestClockConfig.class)
public abstract class IntegrationTestBase {

    @Autowired protected ParkingService parkingService;
    @Autowired protected FloorAdminService floorAdminService;
    @Autowired protected AvailabilityService availabilityService;
    @Autowired protected MutableClock clock;
    @Autowired protected ParkingTicketRepository ticketRepository;
    @Autowired protected VehicleRepository vehicleRepository;
    @Autowired protected ParkingSpotRepository spotRepository;
    @Autowired protected ParkingFloorRepository floorRepository;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAllInBatch();
        vehicleRepository.deleteAllInBatch();
        spotRepository.deleteAllInBatch();
        floorRepository.deleteAllInBatch();
        clock.reset();
    }
}
