package com.smartparking.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Test clock that only moves when told to. */
public class MutableClock extends Clock {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T10:00:00Z"));

    public void advance(Duration d) {
        now.updateAndGet(i -> i.plus(d));
    }

    public void reset() {
        now.set(Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now.get(); }
}
