package com.example.booking.repository;

import com.example.booking.model.Ticket;
import com.example.booking.model.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    boolean existsByTicketCodeIgnoreCase(String ticketCode);

    boolean existsByTripId(Long tripId);

    List<Ticket> findByTripIdAndStatusIn(Long tripId, Collection<TicketStatus> statuses);

    List<Ticket> findByStatusAndCreatedAtBefore(TicketStatus status, LocalDateTime threshold);

    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByStatus(TicketStatus status);

    long countByStatusIn(Collection<TicketStatus> statuses);

    long countByCreatedAtBetweenAndStatusIn(LocalDateTime start, LocalDateTime end, Collection<TicketStatus> statuses);

    @Query("""
            select concat(t.trip.origin, ' - ', t.trip.destination), sum(t.totalAmount)
            from Ticket t
            where t.status in ('PAID', 'COMPLETED')
            group by t.trip.origin, t.trip.destination
            order by sum(t.totalAmount) desc
            """)
    List<Object[]> sumRevenueByRoute();
}
