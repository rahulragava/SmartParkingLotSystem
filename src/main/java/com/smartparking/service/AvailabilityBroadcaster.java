package com.smartparking.service;

import com.smartparking.web.dto.AvailabilityResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Pushes availability snapshots to connected Server-Sent-Event clients. */
@Component
public class AvailabilityBroadcaster {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(AvailabilityResponse initial) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        send(emitter, initial);
        return emitter;
    }

    public boolean hasSubscribers() {
        return !emitters.isEmpty();
    }

    public void broadcast(AvailabilityResponse snapshot) {
        for (SseEmitter emitter : emitters) {
            send(emitter, snapshot);
        }
    }

    private void send(SseEmitter emitter, AvailabilityResponse snapshot) {
        try {
            emitter.send(SseEmitter.event().name("availability").data(snapshot));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(emitter);
        }
    }
}
