package com.example.booking.service.impl;

import com.example.booking.dto.BusRequest;
import com.example.booking.dto.BusResponse;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.Bus;
import com.example.booking.model.BusCompany;
import com.example.booking.model.Seat;
import com.example.booking.model.enums.BusStatus;
import com.example.booking.model.enums.CompanyStatus;
import com.example.booking.model.enums.SeatType;
import com.example.booking.repository.BusCompanyRepository;
import com.example.booking.repository.BusRepository;
import com.example.booking.repository.BusTripRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.service.BusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BusServiceImpl implements BusService {

    /** So ghe VIP o dau moi tang. */
    private static final int VIP_SEATS_PER_FLOOR = 4;

    private final BusRepository busRepository;
    private final BusCompanyRepository companyRepository;
    private final SeatRepository seatRepository;
    private final BusTripRepository tripRepository;

    public BusServiceImpl(BusRepository busRepository, BusCompanyRepository companyRepository,
                          SeatRepository seatRepository, BusTripRepository tripRepository) {
        this.busRepository = busRepository;
        this.companyRepository = companyRepository;
        this.seatRepository = seatRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusResponse> findByCompany(Long companyId) {
        return busRepository.findByCompanyIdOrderByLicensePlateAsc(companyId).stream().map(BusResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusResponse> findAssignable() {
        return busRepository.findByStatusAndCompanyStatusOrderByCompanyNameAscLicensePlateAsc(
                BusStatus.ACTIVE, CompanyStatus.ACTIVE).stream().map(BusResponse::from).toList();
    }

    @Override
    @Transactional
    public BusResponse create(BusRequest r) {
        BusCompany company = companyRepository.findById(r.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhà xe", r.getCompanyId()));
        String plate = r.getLicensePlate().trim().toUpperCase();
        if (busRepository.existsByLicensePlateIgnoreCase(plate)) {
            throw new BusinessException("Biển số xe đã tồn tại");
        }
        int floors = r.getFloors();
        if (r.getTotalSeats() % floors != 0) {
            throw new BusinessException("Tổng số ghế phải chia hết cho số tầng");
        }

        Bus bus = new Bus();
        bus.setCompany(company);
        bus.setLicensePlate(plate);
        bus.setBusType(r.getBusType());
        bus.setTotalSeats(r.getTotalSeats());
        bus.setFloors(floors);
        bus = busRepository.save(bus);

        seatRepository.saveAll(generateSeats(bus));
        return BusResponse.from(bus);
    }

    @Override
    @Transactional
    public void delete(Long busId) {
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new ResourceNotFoundException("Xe", busId));
        if (tripRepository.existsByBusId(busId)) {
            throw new BusinessException("Xe đã có chuyến chạy, không thể xóa");
        }
        busRepository.delete(bus); // seats bi xoa theo (ON DELETE CASCADE)
    }

    /** Tang 1: A01.. ; tang 2: B01.. ; VIP_SEATS_PER_FLOOR ghe dau moi tang la VIP. */
    private List<Seat> generateSeats(Bus bus) {
        int perFloor = bus.getTotalSeats() / bus.getFloors();
        List<Seat> seats = new ArrayList<>();
        for (int floor = 1; floor <= bus.getFloors(); floor++) {
            char prefix = (char) ('A' + floor - 1);
            for (int i = 1; i <= perFloor; i++) {
                Seat s = new Seat();
                s.setBus(bus);
                s.setFloorNo(floor);
                s.setSeatCode(String.format("%c%02d", prefix, i));
                s.setSeatType(i <= VIP_SEATS_PER_FLOOR ? SeatType.VIP : SeatType.NORMAL);
                seats.add(s);
            }
        }
        return seats;
    }
}
