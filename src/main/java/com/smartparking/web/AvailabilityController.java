package com.smartparking.web;

import com.smartparking.service.AvailabilityBroadcaster;
import com.smartparking.service.AvailabilityService;
import com.smartparking.web.dto.AvailabilityResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/parking/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final AvailabilityBroadcaster broadcaster;

    public AvailabilityController(AvailabilityService availabilityService, AvailabilityBroadcaster broadcaster) {
        this.availabilityService = availabilityService;
        this.broadcaster = broadcaster;
    }

    @GetMapping
    public AvailabilityResponse availability() {
        return availabilityService.snapshot();
    }

    /** Server-Sent Events: current snapshot immediately, then one event after every committed change. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return broadcaster.subscribe(availabilityService.snapshot());
    }
}
