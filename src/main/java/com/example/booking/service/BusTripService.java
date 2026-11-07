package com.example.booking.service;

import com.example.booking.dto.BusTripRequest;
import com.example.booking.dto.BusTripResponse;
import com.example.booking.dto.TripSearchRequest;
import com.example.booking.model.enums.TripStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BusTripService {

    Page<BusTripResponse> search(String keyword, TripStatus status, int page, int size);

    List<BusTripResponse> findByCompany(Long companyId);

    BusTripResponse getById(Long id);

    BusTripResponse save(BusTripRequest request);

    /** Huy chuyen: huy toan bo ve dang hieu luc, hoan tien cac thanh toan thanh cong, nha ghe giu tam. */
    void cancel(Long id);

    /** Chi xoa duoc chuyen chua co ve nao. */
    void delete(Long id);

    // =========================================================================
    // Cac phuong thuc tim kiem va hien thi cho Khach hang (Giai doan 5)
    // =========================================================================

    /** Tim kiem va loc chuyen xe cho khach hang voi cac bo loc nang cao */
    Page<BusTripResponse> searchCustomerTrips(TripSearchRequest request);

    /** Danh sach chuyen xe de xuat tren trang chu */
    List<BusTripResponse> getRecommendedTrips();

    /** Danh sach diem di duy nhat */
    List<String> getDistinctOrigins();

    /** Danh sach diem den duy nhat */
    List<String> getDistinctDestinations();
}
