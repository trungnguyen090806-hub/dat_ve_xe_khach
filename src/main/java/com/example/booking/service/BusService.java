package com.example.booking.service;

import com.example.booking.dto.BusRequest;
import com.example.booking.dto.BusResponse;

import java.util.List;

public interface BusService {

    List<BusResponse> findByCompany(Long companyId);

    /** Danh sach xe dang hoat dong (cua nha xe dang hoat dong) de chon khi tao chuyen. */
    List<BusResponse> findAssignable();

    /** Tao xe va tu dong sinh so do ghe (A01.., B01..; 4 ghe dau moi tang la VIP). */
    BusResponse create(BusRequest request);

    void delete(Long busId);
}
