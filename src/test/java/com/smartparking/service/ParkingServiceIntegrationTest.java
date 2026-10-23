package com.smartparking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartparking.domain.SpotType;
import com.smartparking.domain.TicketStatus;
import com.smartparking.domain.VehicleType;
import com.smartparking.web.dto.AvailabilityResponse;
import com.smartparking.web.dto.CreateFloorRequest;
import com.smartparking.web.dto.TicketResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ParkingServiceIntegrationTest extends IntegrationTestBase {

    private void lot(int floor, int small, int medium, int large) {
        floorAdminService.createFloor(new CreateFloorRequest(floor, small, medium, large));
    }

    @Test
    void vehiclesGetSmallestCompatibleSpot() {
        lot(0, 1, 1, 1);

        assertThat(parkingService.checkIn("MC-1", VehicleType.MOTORCYCLE).spotType()).isEqualTo(SpotType.SMALL);
        assertThat(parkingService.checkIn("CAR-1", VehicleType.CAR).spotType()).isEqualTo(SpotType.MEDIUM);
        assertThat(parkingService.checkIn("BUS-1", VehicleType.BUS).spotType()).isEqualTo(SpotType.LARGE);
    }

    @Test
    void overflowGoesToNextLargerSpotType() {
        lot(0, 0, 1, 1);

        assertThat(parkingService.checkIn("MC-1", VehicleType.MOTORCYCLE).spotType()).isEqualTo(SpotType.MEDIUM);
        assertThat(parkingService.checkIn("CAR-1", VehicleType.CAR).spotType()).isEqualTo(SpotType.LARGE);
    }

    @Test
    void lowestFloorIsPreferred() {
        lot(2, 0, 1, 0);
        lot(1, 0, 1, 0);

        assertThat(parkingService.checkIn("CAR-1", VehicleType.CAR).floorNumber()).isEqualTo(1);
        assertThat(parkingService.checkIn("CAR-2", VehicleType.CAR).floorNumber()).isEqualTo(2);
    }

    @Test
    void busCannotUseSmallerSpots() {
        lot(0, 5, 5, 0);

        assertThatThrownBy(() -> parkingService.checkIn("BUS-1", VehicleType.BUS))
                .isInstanceOf(ParkingException.class).hasMessageContaining("No free spot");
    }

    @Test
    void sameVehicleCannotCheckInTwice() {
        lot(0, 0, 5, 0);
        parkingService.checkIn("car 1", VehicleType.CAR);

        assertThatThrownBy(() -> parkingService.checkIn("CAR-1", VehicleType.CAR)) // normalised plate
                .isInstanceOf(ParkingException.class).hasMessageContaining("already parked");
    }

    @Test
    void checkOutChargesFeeFreesSpotAndAllowsReentry() {
        lot(0, 0, 1, 0);
        TicketResponse in = parkingService.checkIn("CAR-1", VehicleType.CAR);
        assertThat(availabilityService.snapshot().availableSpots()).isZero();

        clock.advance(Duration.ofMinutes(90));
        TicketResponse out = parkingService.checkOut(in.ticketNumber());

        assertThat(out.status()).isEqualTo(TicketStatus.COMPLETED);
        assertThat(out.fee()).isEqualByComparingTo("40"); // 2 started hours * 20
        assertThat(out.durationMinutes()).isEqualTo(90);
        assertThat(availabilityService.snapshot().availableSpots()).isEqualTo(1);

        assertThat(parkingService.checkIn("CAR-1", VehicleType.CAR).ticketNumber()).isNotEqualTo(in.ticketNumber());
    }

    @Test
    void feeDependsOnVehicleType() {
        lot(0, 1, 0, 1);
        TicketResponse bus = parkingService.checkIn("BUS-1", VehicleType.BUS);
        TicketResponse bike = parkingService.checkIn("MC-1", VehicleType.MOTORCYCLE);
        clock.advance(Duration.ofHours(1));

        assertThat(parkingService.checkOut(bus.ticketNumber()).fee()).isEqualByComparingTo("50");
        assertThat(parkingService.checkOut(bike.ticketNumber()).fee()).isEqualByComparingTo("10");
    }

    @Test
    void checkingOutTwiceIsRejected() {
        lot(0, 0, 1, 0);
        TicketResponse in = parkingService.checkIn("CAR-1", VehicleType.CAR);
        parkingService.checkOut(in.ticketNumber());

        assertThatThrownBy(() -> parkingService.checkOut(in.ticketNumber()))
                .isInstanceOf(ParkingException.class).hasMessageContaining("already checked out");
    }

    @Test
    void unknownTicketIsNotFound() {
        assertThatThrownBy(() -> parkingService.checkOut("NOPE"))
                .isInstanceOf(ParkingException.class).hasMessageContaining("not found");
        assertThatThrownBy(() -> parkingService.getTicket("NOPE"))
                .isInstanceOf(ParkingException.class);
    }

    @Test
    void activeTicketShowsRunningFeeEstimate() {
        lot(0, 0, 1, 0);
        TicketResponse in = parkingService.checkIn("CAR-1", VehicleType.CAR);
        clock.advance(Duration.ofMinutes(61));

        TicketResponse view = parkingService.getTicket(in.ticketNumber());
        assertThat(view.status()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(view.fee()).isEqualByComparingTo("40");
        assertThat(view.exitTime()).isNull();
    }

    @Test
    void availabilityIsBrokenDownByTypeFloorAndVehicle() {
        lot(0, 1, 2, 1);
        lot(1, 0, 1, 0);
        parkingService.checkIn("CAR-1", VehicleType.CAR);

        AvailabilityResponse a = availabilityService.snapshot();
        assertThat(a.totalSpots()).isEqualTo(5);
        assertThat(a.availableSpots()).isEqualTo(4);
        assertThat(a.byFloor()).hasSize(2);
        assertThat(a.availableForVehicleType().get(VehicleType.CAR)).isEqualTo(3);       // 2 medium + 1 large
        assertThat(a.availableForVehicleType().get(VehicleType.BUS)).isEqualTo(1);
        assertThat(a.availableForVehicleType().get(VehicleType.MOTORCYCLE)).isEqualTo(4);
    }

    @Test
    void duplicateFloorOrEmptyFloorRejected() {
        lot(0, 1, 0, 0);
        assertThatThrownBy(() -> lot(0, 1, 0, 0)).isInstanceOf(ParkingException.class);
        assertThatThrownBy(() -> lot(5, 0, 0, 0)).isInstanceOf(ParkingException.class);
    }
}
