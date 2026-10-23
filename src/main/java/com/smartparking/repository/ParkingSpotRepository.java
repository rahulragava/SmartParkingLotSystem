package com.smartparking.repository;

import com.smartparking.domain.ParkingSpot;
import com.smartparking.domain.SpotType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    /**
     * Picks free spots of one type (lowest floor, then spot number) and row-locks them with
     * {@code SELECT ... FOR UPDATE SKIP LOCKED} (lock.timeout = -2). Concurrent check-ins therefore
     * never block on, or double-book, the same spot: each transaction receives a different row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select s from ParkingSpot s where s.occupied = false and s.spotType = :type "
            + "order by s.floorNumber asc, s.spotNumber asc")
    List<ParkingSpot> findFreeForUpdate(@Param("type") SpotType type, Pageable pageable);

    /** Non-locking count, used to tell "really full" from "free spots exist but are momentarily locked". */
    @Query("select count(s) from ParkingSpot s where s.occupied = false and s.spotType in :types")
    long countFree(@Param("types") Collection<SpotType> types);

    @Query("select s.floorNumber as floorNumber, s.spotType as spotType, count(s) as total, "
            + "sum(case when s.occupied = false then 1L else 0L end) as available "
            + "from ParkingSpot s group by s.floorNumber, s.spotType order by s.floorNumber, s.spotType")
    List<SpotCountProjection> countByFloorAndType();
}
