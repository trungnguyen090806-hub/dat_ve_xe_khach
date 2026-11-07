package com.example.booking.repository;

import com.example.booking.model.Bus;
import com.example.booking.model.enums.BusStatus;
import com.example.booking.model.enums.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {

    List<Bus> findByCompanyIdOrderByLicensePlateAsc(Long companyId);

    boolean existsByCompanyId(Long companyId);

    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    long countByCompanyId(Long companyId);

    /** Xe dang hoat dong cua nha xe dang hoat dong (dung cho dropdown khi tao chuyen). */
    List<Bus> findByStatusAndCompanyStatusOrderByCompanyNameAscLicensePlateAsc(BusStatus status, CompanyStatus companyStatus);
}
