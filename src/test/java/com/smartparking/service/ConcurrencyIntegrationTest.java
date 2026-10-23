package com.smartparking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartparking.domain.TicketStatus;
import com.smartparking.domain.VehicleType;
import com.smartparking.web.dto.CreateFloorRequest;
import com.smartparking.web.dto.TicketResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ConcurrencyIntegrationTest extends IntegrationTestBase {

    private static final int THREADS = 12;

    /** Runs all tasks at the same instant and returns their results (value or exception). */
    private <T> List<Object> runConcurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    return task.call();
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        start.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<Object> f : futures) {
            results.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdownNow();
        return results;
    }

    @Test
    void simultaneousCheckInsNeverOverbookAndNeverShareASpot() throws Exception {
        int spots = 3;
        floorAdminService.createFloor(new CreateFloorRequest(0, 0, spots, 0));

        List<Callable<TicketResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String plate = "CAR-" + i;
            tasks.add(() -> parkingService.checkIn(plate, VehicleType.CAR));
        }
        List<Object> results = runConcurrently(tasks);

        List<TicketResponse> ok = results.stream().filter(r -> r instanceof TicketResponse)
                .map(r -> (TicketResponse) r).toList();
        List<Object> failed = results.stream().filter(r -> !(r instanceof TicketResponse)).toList();

        assertThat(ok).hasSize(spots);
        assertThat(failed).hasSize(THREADS - spots).allMatch(e -> e instanceof ParkingException);
        Set<String> distinctSpots = ok.stream().map(TicketResponse::spotNumber).collect(Collectors.toSet());
        assertThat(distinctSpots).hasSize(spots);
        assertThat(availabilityService.snapshot().availableSpots()).isZero();
    }

    @Test
    void simultaneousCheckInsOfSamePlateCreateOnlyOneSession() throws Exception {
        floorAdminService.createFloor(new CreateFloorRequest(0, 0, 10, 0));

        List<Callable<TicketResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tasks.add(() -> parkingService.checkIn("SAME-1", VehicleType.CAR));
        }
        List<Object> results = runConcurrently(tasks);

        assertThat(results.stream().filter(r -> r instanceof TicketResponse)).hasSize(1);
        assertThat(ticketRepository.findAll().stream()
                .filter(t -> t.getStatus() == TicketStatus.ACTIVE)).hasSize(1);
        assertThat(availabilityService.snapshot().availableSpots()).isEqualTo(9);
    }

    @Test
    void simultaneousCheckOutsOfSameTicketSucceedOnce() throws Exception {
        floorAdminService.createFloor(new CreateFloorRequest(0, 0, 2, 0));
        TicketResponse in = parkingService.checkIn("CAR-1", VehicleType.CAR);

        List<Callable<TicketResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tasks.add(() -> parkingService.checkOut(in.ticketNumber()));
        }
        List<Object> results = runConcurrently(tasks);

        assertThat(results.stream().filter(r -> r instanceof TicketResponse)).hasSize(1);
        assertThat(availabilityService.snapshot().availableSpots()).isEqualTo(2);
    }
}
