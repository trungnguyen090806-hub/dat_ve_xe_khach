package com.example.booking.repository;

import com.example.booking.model.TicketDetail;
import com.example.booking.model.enums.TicketDetailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface TicketDetailRepository extends JpaRepository<TicketDetail, Long> {

    long countByTripIdAndStatus(Long tripId, TicketDetailStatus status);

    long countByStatus(TicketDetailStatus status);

    @Query("select td.seat.id from TicketDetail td where td.trip.id = :tripId and td.status = 'ACTIVE'")
    Set<Long> findBookedSeatIds(@Param("tripId") Long tripId);

    boolean existsByTripIdAndSeatIdAndStatus(Long tripId, Long seatId, TicketDetailStatus status);
}
