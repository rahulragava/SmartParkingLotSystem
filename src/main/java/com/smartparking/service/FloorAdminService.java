package com.smartparking.service;

import com.smartparking.domain.ParkingFloor;
import com.smartparking.domain.ParkingSpot;
import com.smartparking.domain.SpotType;
import com.smartparking.event.AvailabilityChangedEvent;
import com.smartparking.repository.ParkingFloorRepository;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.web.dto.CreateFloorRequest;
import com.smartparking.web.dto.FloorResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FloorAdminService {

    private final ParkingFloorRepository floorRepository;
    private final ParkingSpotRepository spotRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FloorAdminService(ParkingFloorRepository floorRepository,
                             ParkingSpotRepository spotRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.floorRepository = floorRepository;
        this.spotRepository = spotRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FloorResponse createFloor(CreateFloorRequest req) {
        if (req.smallSpots() + req.mediumSpots() + req.largeSpots() == 0) {
            throw ParkingException.invalid("A floor needs at least one spot");
        }
        if (floorRepository.existsByFloorNumber(req.floorNumber())) {
            throw ParkingException.floorExists(req.floorNumber());
        }
        ParkingFloor floor = floorRepository.save(new ParkingFloor(req.floorNumber()));

        List<ParkingSpot> spots = new ArrayList<>();
        addSpots(spots, floor, SpotType.SMALL, req.smallSpots());
        addSpots(spots, floor, SpotType.MEDIUM, req.mediumSpots());
        addSpots(spots, floor, SpotType.LARGE, req.largeSpots());
        spotRepository.saveAll(spots);

        eventPublisher.publishEvent(new AvailabilityChangedEvent());
        return new FloorResponse(req.floorNumber(), req.smallSpots(), req.mediumSpots(), req.largeSpots());
    }

    private static void addSpots(List<ParkingSpot> out, ParkingFloor floor, SpotType type, int count) {
        for (int i = 1; i <= count; i++) {
            out.add(new ParkingSpot(String.format("F%d-%s%03d", floor.getFloorNumber(), type.code(), i), type, floor));
        }
    }
}
