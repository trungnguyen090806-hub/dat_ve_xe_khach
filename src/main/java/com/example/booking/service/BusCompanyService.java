package com.example.booking.service;

import com.example.booking.dto.BusCompanyRequest;
import com.example.booking.dto.BusCompanyResponse;
import com.example.booking.model.enums.CompanyStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BusCompanyService {

    Page<BusCompanyResponse> search(String keyword, CompanyStatus status, int page, int size);

    BusCompanyResponse getById(Long id);

    BusCompanyResponse save(BusCompanyRequest request);

    /** Chi xoa duoc khi nha xe chua co xe nao. */
    void delete(Long id);

    /** Danh sach nha xe noi bat (danh gia cao nhat) cho trang chu */
    List<BusCompanyResponse> getFeaturedCompanies();

    /** Danh sach tat ca nha xe dang hoat dong de loc tim kiem */
    List<BusCompanyResponse> getActiveCompanies();
}
