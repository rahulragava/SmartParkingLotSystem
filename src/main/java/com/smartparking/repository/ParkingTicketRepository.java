package com.smartparking.repository;

import com.smartparking.domain.ParkingTicket;
import com.smartparking.domain.TicketStatus;
import com.smartparking.domain.Vehicle;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, Long> {

    boolean existsByVehicleAndStatus(Vehicle vehicle, TicketStatus status);

    @Query("select t from ParkingTicket t join fetch t.spot join fetch t.vehicle where t.ticketNumber = :n")
    Optional<ParkingTicket> findDetailedByTicketNumber(@Param("n") String ticketNumber);

    /** Row-locks the ticket so two simultaneous check-outs of the same ticket are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ParkingTicket t where t.ticketNumber = :n")
    Optional<ParkingTicket> findByTicketNumberForUpdate(@Param("n") String ticketNumber);
}
