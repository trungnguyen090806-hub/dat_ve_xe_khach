package com.example.booking.repository;

import com.example.booking.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByTicketId(Long ticketId);

    List<Payment> findByTicketUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = 'SUCCESS'")
    BigDecimal sumTotalSuccessfulAmount();

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.status = 'SUCCESS' and p.paidAt >= :start and p.paidAt < :end
            """)
    BigDecimal sumRevenueBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
