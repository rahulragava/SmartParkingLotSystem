package com.smartparking.web;

import com.smartparking.service.FloorAdminService;
import com.smartparking.web.dto.CreateFloorRequest;
import com.smartparking.web.dto.FloorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Lot configuration. NOTE: not secured; put behind authentication before exposing publicly. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final FloorAdminService floorAdminService;

    public AdminController(FloorAdminService floorAdminService) {
        this.floorAdminService = floorAdminService;
    }

    @PostMapping("/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorResponse createFloor(@Valid @RequestBody CreateFloorRequest request) {
        return floorAdminService.createFloor(request);
    }
}
