package com.smartparking.web;

import com.smartparking.service.ParkingService;
import com.smartparking.web.dto.CheckInRequest;
import com.smartparking.web.dto.CheckOutRequest;
import com.smartparking.web.dto.TicketResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/parking")
public class ParkingController {

    private final ParkingService parkingService;

    public ParkingController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return parkingService.checkIn(request.licensePlate(), request.vehicleType());
    }

    @PostMapping("/check-out")
    public TicketResponse checkOut(@Valid @RequestBody CheckOutRequest request) {
        return parkingService.checkOut(request.ticketNumber());
    }

    @GetMapping("/tickets/{ticketNumber}")
    public TicketResponse getTicket(@PathVariable String ticketNumber) {
        return parkingService.getTicket(ticketNumber);
    }
}
