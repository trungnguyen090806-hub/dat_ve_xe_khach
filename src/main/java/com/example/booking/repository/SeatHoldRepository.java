package com.example.booking.repository;

import com.example.booking.model.SeatHold;
import com.example.booking.model.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SeatHold h set h.status = :to where h.trip.id = :tripId and h.status = :from")
    int updateStatusByTrip(@Param("tripId") Long tripId, @Param("from") HoldStatus from, @Param("to") HoldStatus to);

    long countByTripIdAndStatusAndExpiresAtAfter(Long tripId, HoldStatus status, LocalDateTime now);

    List<SeatHold> findByTripIdAndUserIdAndStatusAndExpiresAtAfter(Long tripId, Long userId, HoldStatus status, LocalDateTime now);

    @Query("select h from SeatHold h where h.trip.id = :tripId and h.status = 'HELD' and h.expiresAt > :now")
    List<SeatHold> findActiveHoldsByTrip(@Param("tripId") Long tripId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SeatHold h set h.status = 'EXPIRED' where h.status = 'HELD' and h.expiresAt <= :now")
    int expireOutdatedHolds(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SeatHold h set h.status = 'RELEASED' where h.trip.id = :tripId and h.user.id = :userId and h.status = 'HELD'")
    int releaseUserHolds(@Param("tripId") Long tripId, @Param("userId") Long userId);

    @Query("""
            select count(h) > 0 from SeatHold h
            where h.trip.id = :tripId and h.seat.id in :seatIds
              and h.status = 'HELD' and h.expiresAt > :now
              and (:userId is null or h.user.id <> :userId)
            """)
    boolean existsHeldByOthers(@Param("tripId") Long tripId,
                               @Param("seatIds") Collection<Long> seatIds,
                               @Param("now") LocalDateTime now,
                               @Param("userId") Long userId);
}
