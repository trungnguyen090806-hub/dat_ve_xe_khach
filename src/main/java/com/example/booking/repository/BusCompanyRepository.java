package com.example.booking.repository;

import com.example.booking.model.BusCompany;
import com.example.booking.model.enums.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusCompanyRepository extends JpaRepository<BusCompany, Long>, JpaSpecificationExecutor<BusCompany> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<BusCompany> findByStatusOrderByNameAsc(CompanyStatus status);

    List<BusCompany> findTop6ByStatusOrderByRatingAvgDesc(CompanyStatus status);
}
