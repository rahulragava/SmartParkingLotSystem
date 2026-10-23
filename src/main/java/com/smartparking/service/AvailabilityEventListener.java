package com.smartparking.service;

import com.smartparking.event.AvailabilityChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Broadcasts a fresh snapshot only after the originating transaction committed (never for rolled-back changes). */
@Component
public class AvailabilityEventListener {

    private final AvailabilityService availabilityService;
    private final AvailabilityBroadcaster broadcaster;

    public AvailabilityEventListener(AvailabilityService availabilityService, AvailabilityBroadcaster broadcaster) {
        this.availabilityService = availabilityService;
        this.broadcaster = broadcaster;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onChange(AvailabilityChangedEvent event) {
        if (broadcaster.hasSubscribers()) {
            broadcaster.broadcast(availabilityService.snapshot());
        }
    }
}
