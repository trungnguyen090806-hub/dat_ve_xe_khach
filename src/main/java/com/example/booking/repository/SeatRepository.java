package com.example.booking.repository;

import com.example.booking.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByBusIdOrderByFloorNoAscSeatCodeAsc(Long busId);

    Optional<Seat> findByBusIdAndSeatCode(Long busId, String seatCode);
}
