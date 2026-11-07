package com.example.booking.service.impl;

import com.example.booking.dto.BusTripRequest;
import com.example.booking.dto.BusTripResponse;
import com.example.booking.dto.TripSearchRequest;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.Bus;
import com.example.booking.model.BusTrip;
import com.example.booking.model.Payment;
import com.example.booking.model.Ticket;
import com.example.booking.model.TicketDetail;
import com.example.booking.model.enums.BusStatus;
import com.example.booking.model.enums.CompanyStatus;
import com.example.booking.model.enums.HoldStatus;
import com.example.booking.model.enums.PaymentStatus;
import com.example.booking.model.enums.TicketDetailStatus;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.model.enums.TripStatus;
import com.example.booking.repository.BusRepository;
import com.example.booking.repository.BusTripRepository;
import com.example.booking.repository.PaymentRepository;
import com.example.booking.repository.SeatHoldRepository;
import com.example.booking.repository.TicketDetailRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.service.BusTripService;
import com.example.booking.utils.CodeGenerator;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BusTripServiceImpl implements BusTripService {

    private final BusTripRepository tripRepository;
    private final BusRepository busRepository;
    private final TicketRepository ticketRepository;
    private final TicketDetailRepository ticketDetailRepository;
    private final PaymentRepository paymentRepository;
    private final SeatHoldRepository seatHoldRepository;

    public BusTripServiceImpl(BusTripRepository tripRepository, BusRepository busRepository,
                              TicketRepository ticketRepository, TicketDetailRepository ticketDetailRepository,
                              PaymentRepository paymentRepository, SeatHoldRepository seatHoldRepository) {
        this.tripRepository = tripRepository;
        this.busRepository = busRepository;
        this.ticketRepository = ticketRepository;
        this.ticketDetailRepository = ticketDetailRepository;
        this.paymentRepository = paymentRepository;
        this.seatHoldRepository = seatHoldRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BusTripResponse> search(String keyword, TripStatus status, int page, int size) {
        Specification<BusTrip> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                Join<?, ?> company = root.join("bus").join("company");
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("tripCode")), like),
                        cb.like(cb.lower(root.get("origin")), like),
                        cb.like(cb.lower(root.get("destination")), like),
                        cb.like(cb.lower(company.get("name")), like)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return tripRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "departureTime")))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusTripResponse> findByCompany(Long companyId) {
        return tripRepository.findByBusCompanyIdOrderByDepartureTimeDesc(companyId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BusTripResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public BusTripResponse save(BusTripRequest r) {
        validateBusinessRules(r);

        boolean creating = r.getId() == null;
        BusTrip trip;
        if (creating) {
            trip = new BusTrip();
        } else {
            trip = find(r.getId());
            if (trip.getStatus() == TripStatus.CANCELLED || trip.getStatus() == TripStatus.COMPLETED) {
                throw new BusinessException("Không thể sửa chuyến đã hủy hoặc đã hoàn thành");
            }
        }

        Bus bus = busRepository.findById(r.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Xe", r.getBusId()));
        boolean busChanged = trip.getBus() == null || !trip.getBus().getId().equals(bus.getId());
        if (busChanged) {
            if (bus.getStatus() != BusStatus.ACTIVE || bus.getCompany().getStatus() != CompanyStatus.ACTIVE) {
                throw new BusinessException("Xe hoặc nhà xe đang không hoạt động");
            }
            if (!creating && ticketRepository.existsByTripId(trip.getId())) {
                throw new BusinessException("Chuyến đã có vé, không thể đổi xe");
            }
        }
        if (tripRepository.existsOverlap(bus.getId(), r.getDepartureTime(), r.getArrivalTime(),
                creating ? null : trip.getId(), TripStatus.CANCELLED)) {
            throw new BusinessException("Xe đã có chuyến khác trùng khung giờ này");
        }

        String code = (r.getTripCode() == null || r.getTripCode().isBlank())
                ? (creating ? generateUniqueCode() : trip.getTripCode())
                : r.getTripCode().trim().toUpperCase();
        boolean duplicateCode = creating
                ? tripRepository.existsByTripCodeIgnoreCase(code)
                : tripRepository.existsByTripCodeIgnoreCaseAndIdNot(code, trip.getId());
        if (duplicateCode) {
            throw new BusinessException("Mã chuyến đã tồn tại");
        }

        trip.setTripCode(code);
        trip.setBus(bus);
        trip.setOrigin(r.getOrigin().trim());
        trip.setDestination(r.getDestination().trim());
        trip.setPickupPoint(blankToNull(r.getPickupPoint()));
        trip.setDropoffPoint(blankToNull(r.getDropoffPoint()));
        trip.setDepartureTime(r.getDepartureTime());
        trip.setArrivalTime(r.getArrivalTime());
        trip.setBasePrice(r.getBasePrice());
        trip.setVipSurcharge(r.getVipSurcharge());
        if (r.getStatus() != null && r.getStatus() != TripStatus.CANCELLED) {
            trip.setStatus(r.getStatus());
        }
        return toResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        BusTrip trip = find(id);
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new BusinessException("Chuyến xe đã được hủy trước đó");
        }
        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new BusinessException("Không thể hủy chuyến đã hoàn thành");
        }
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> tickets = ticketRepository.findByTripIdAndStatusIn(id,
                List.of(TicketStatus.PENDING, TicketStatus.PAID));
        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticket.setCancelReason("Nhà xe hủy chuyến");
            ticket.setCancelledAt(now);
            for (TicketDetail d : ticket.getDetails()) {
                d.setStatus(TicketDetailStatus.CANCELLED); // active_key = NULL -> nha ghe
            }
            for (Payment p : paymentRepository.findByTicketId(ticket.getId())) {
                if (p.getStatus() == PaymentStatus.SUCCESS) {
                    p.setStatus(PaymentStatus.REFUNDED);
                    p.setNote("Hoàn tiền do nhà xe hủy chuyến");
                }
            }
        }
        ticketRepository.saveAll(tickets);
        seatHoldRepository.updateStatusByTrip(id, HoldStatus.HELD, HoldStatus.RELEASED);
        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BusTrip trip = find(id);
        if (ticketRepository.existsByTripId(id)) {
            throw new BusinessException("Chuyến đã có vé, không thể xóa. Hãy dùng chức năng Hủy chuyến.");
        }
        tripRepository.delete(trip);
    }

    // =========================================================================
    // Cac phuong thuc tim kiem va hien thi cho Khach hang (Giai doan 5)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<BusTripResponse> searchCustomerTrips(TripSearchRequest req) {
        LocalDateTime now = LocalDateTime.now();

        Specification<BusTrip> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chi tim cac chuyen SCHEDULED va chua khoi hanh
            predicates.add(cb.equal(root.get("status"), TripStatus.SCHEDULED));
            predicates.add(cb.greaterThan(root.get("departureTime"), now));

            // 2. Loc diem di
            if (req.getOrigin() != null && !req.getOrigin().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("origin")), "%" + req.getOrigin().trim().toLowerCase() + "%"));
            }

            // 3. Loc diem den
            if (req.getDestination() != null && !req.getDestination().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("destination")), "%" + req.getDestination().trim().toLowerCase() + "%"));
            }

            // 4. Loc theo ngay khoi hanh
            if (req.getDepartureDate() != null) {
                LocalDate date = req.getDepartureDate();
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("departureTime"), startOfDay.isBefore(now) ? now : startOfDay));
                predicates.add(cb.lessThan(root.get("departureTime"), endOfDay));
            }

            // 5. Loc theo gia ve
            if (req.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), req.getMinPrice()));
            }
            if (req.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), req.getMaxPrice()));
            }

            // 6. Loc theo loai xe
            if (req.getBusType() != null) {
                predicates.add(cb.equal(root.join("bus").get("busType"), req.getBusType()));
            }

            // 7. Loc theo nha xe
            if (req.getCompanyId() != null) {
                predicates.add(cb.equal(root.join("bus").join("company").get("id"), req.getCompanyId()));
            }

            // 8. Loc theo khung gio khoi hanh
            if (req.getTimeRange() != null && !req.getTimeRange().isBlank()) {
                Expression<Integer> hour = cb.function("hour", Integer.class, root.get("departureTime"));
                switch (req.getTimeRange().toUpperCase()) {
                    case "EARLY_MORNING" -> predicates.add(cb.and(cb.greaterThanOrEqualTo(hour, 0), cb.lessThan(hour, 6)));
                    case "MORNING" -> predicates.add(cb.and(cb.greaterThanOrEqualTo(hour, 6), cb.lessThan(hour, 12)));
                    case "AFTERNOON" -> predicates.add(cb.and(cb.greaterThanOrEqualTo(hour, 12), cb.lessThan(hour, 18)));
                    case "EVENING" -> predicates.add(cb.and(cb.greaterThanOrEqualTo(hour, 18), cb.lessThan(hour, 24)));
                    default -> {}
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Sap xep
        Sort sort = switch (req.getSortBy() != null ? req.getSortBy() : "time_asc") {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "basePrice");
            case "rating_desc" -> Sort.by(Sort.Direction.DESC, "bus.company.ratingAvg");
            default -> Sort.by(Sort.Direction.ASC, "departureTime");
        };

        int pageSize = req.getSize() > 0 ? req.getSize() : 8;
        int pageIndex = Math.max(0, req.getPage());

        return tripRepository.findAll(spec, PageRequest.of(pageIndex, pageSize, sort))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusTripResponse> getRecommendedTrips() {
        return tripRepository.findTop6ByStatusAndDepartureTimeAfterOrderByDepartureTimeAsc(
                        TripStatus.SCHEDULED, LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctOrigins() {
        return tripRepository.findDistinctOrigins();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctDestinations() {
        return tripRepository.findDistinctDestinations();
    }

    // ---------------------------------------------------------------- helpers

    private void validateBusinessRules(BusTripRequest r) {
        if (r.getBasePrice().signum() <= 0) {
            throw new BusinessException("Giá vé phải lớn hơn 0");
        }
        if (r.getOrigin().trim().equalsIgnoreCase(r.getDestination().trim())) {
            throw new BusinessException("Điểm đi và điểm đến phải khác nhau");
        }
        if (!r.getArrivalTime().isAfter(r.getDepartureTime())) {
            throw new BusinessException("Giờ đến phải sau giờ khởi hành");
        }
        if (r.getId() == null && !r.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Giờ khởi hành phải ở tương lai");
        }
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = CodeGenerator.generate("TR", 6);
        } while (tripRepository.existsByTripCodeIgnoreCase(code));
        return code;
    }

    private BusTrip find(Long id) {
        return tripRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", id));
    }

    private BusTripResponse toResponse(BusTrip t) {
        long bookedSeats = ticketDetailRepository.countByTripIdAndStatus(t.getId(), TicketDetailStatus.ACTIVE);
        long heldSeats = seatHoldRepository.countByTripIdAndStatusAndExpiresAtAfter(t.getId(), HoldStatus.HELD, LocalDateTime.now());
        return BusTripResponse.from(t, bookedSeats + heldSeats);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
