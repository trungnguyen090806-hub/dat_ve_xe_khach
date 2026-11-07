package com.example.booking.repository;

import com.example.booking.model.BusTrip;
import com.example.booking.model.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, Long>, JpaSpecificationExecutor<BusTrip> {

    boolean existsByTripCodeIgnoreCase(String tripCode);

    boolean existsByTripCodeIgnoreCaseAndIdNot(String tripCode, Long id);

    boolean existsByBusId(Long busId);

    long countByBusCompanyId(Long companyId);

    List<BusTrip> findByBusCompanyIdOrderByDepartureTimeDesc(Long companyId);

    /** Lay danh sach diem di duy nhat cua cac chuyen sap khoi hanh */
    @Query("select distinct t.origin from BusTrip t where t.status = 'SCHEDULED' and t.departureTime > CURRENT_TIMESTAMP order by t.origin")
    List<String> findDistinctOrigins();

    /** Lay danh sach diem den duy nhat cua cac chuyen sap khoi hanh */
    @Query("select distinct t.destination from BusTrip t where t.status = 'SCHEDULED' and t.departureTime > CURRENT_TIMESTAMP order by t.destination")
    List<String> findDistinctDestinations();

    /** Danh sach chuyen xe de xuat tren trang chu */
    List<BusTrip> findTop6ByStatusAndDepartureTimeAfterOrderByDepartureTimeAsc(TripStatus status, LocalDateTime now);

    /** Kiem tra xe co bi trung lich (khong tinh chuyen da huy; loai tru chuyen dang sua). */
    @Query("""
            select count(t) > 0 from BusTrip t
            where t.bus.id = :busId and t.status <> :cancelled
              and t.departureTime < :arrival and t.arrivalTime > :departure
              and (:excludeId is null or t.id <> :excludeId)
            """)
    boolean existsOverlap(@Param("busId") Long busId,
                          @Param("departure") LocalDateTime departure,
                          @Param("arrival") LocalDateTime arrival,
                          @Param("excludeId") Long excludeId,
                          @Param("cancelled") TripStatus cancelled);
}
