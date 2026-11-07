package com.example.booking.service.impl;

import com.example.booking.dto.BusTripResponse;
import com.example.booking.dto.SeatDto;
import com.example.booking.dto.TripSeatMapResponse;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.BusTrip;
import com.example.booking.model.Seat;
import com.example.booking.model.SeatHold;
import com.example.booking.model.Ticket;
import com.example.booking.model.TicketDetail;
import com.example.booking.model.User;
import com.example.booking.model.enums.HoldStatus;
import com.example.booking.model.enums.SeatType;
import com.example.booking.model.enums.TicketDetailStatus;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.repository.BusTripRepository;
import com.example.booking.repository.SeatHoldRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.TicketDetailRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.SeatBookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SeatBookingServiceImpl implements SeatBookingService {

    private static final Logger log = LoggerFactory.getLogger(SeatBookingServiceImpl.class);

    private final BusTripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final TicketDetailRepository ticketDetailRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Value("${app.booking.hold-minutes:10}")
    private int holdMinutes;

    public SeatBookingServiceImpl(BusTripRepository tripRepository,
                                  SeatRepository seatRepository,
                                  SeatHoldRepository seatHoldRepository,
                                  TicketDetailRepository ticketDetailRepository,
                                  TicketRepository ticketRepository,
                                  UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.seatRepository = seatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.ticketDetailRepository = ticketDetailRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TripSeatMapResponse getSeatMap(Long tripId, Long currentUserId) {
        BusTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", tripId));

        List<Seat> allSeats = seatRepository.findByBusIdOrderByFloorNoAscSeatCodeAsc(trip.getBus().getId());
        Set<Long> bookedSeatIds = ticketDetailRepository.findBookedSeatIds(tripId);

        LocalDateTime now = LocalDateTime.now();
        List<SeatHold> activeHolds = seatHoldRepository.findActiveHoldsByTrip(tripId, now);
        Map<Long, SeatHold> heldMap = activeHolds.stream()
                .collect(Collectors.toMap(h -> h.getSeat().getId(), h -> h, (h1, h2) -> h1));

        List<SeatDto> floor1 = new ArrayList<>();
        List<SeatDto> floor2 = new ArrayList<>();
        List<Long> myHeldSeatIds = new ArrayList<>();
        LocalDateTime earliestExpiry = null;

        BigDecimal basePrice = trip.getBasePrice();
        BigDecimal vipSurcharge = trip.getVipSurcharge();

        for (Seat s : allSeats) {
            String status;
            String statusLabel;
            boolean canSelect;

            if (bookedSeatIds.contains(s.getId())) {
                status = "BOOKED";
                statusLabel = "Đã đặt";
                canSelect = false;
            } else if (heldMap.containsKey(s.getId())) {
                SeatHold hold = heldMap.get(s.getId());
                boolean isMine = currentUserId != null && hold.getUser().getId().equals(currentUserId);
                if (isMine) {
                    status = "HELD_BY_ME";
                    statusLabel = "Bạn đang giữ";
                    canSelect = true;
                    myHeldSeatIds.add(s.getId());
                    if (earliestExpiry == null || hold.getExpiresAt().isBefore(earliestExpiry)) {
                        earliestExpiry = hold.getExpiresAt();
                    }
                } else {
                    status = "HELD_BY_OTHER";
                    statusLabel = "Đang giữ chỗ";
                    canSelect = false;
                }
            } else {
                status = "AVAILABLE";
                statusLabel = "Còn trống";
                canSelect = true;
            }

            BigDecimal surcharge = s.getSeatType() == SeatType.VIP ? vipSurcharge : BigDecimal.ZERO;
            BigDecimal finalPrice = basePrice.add(surcharge);

            SeatDto dto = SeatDto.builder()
                    .id(s.getId())
                    .seatCode(s.getSeatCode())
                    .floorNo(s.getFloorNo())
                    .seatType(s.getSeatType().name())
                    .seatTypeLabel(s.getSeatType().getLabel())
                    .price(finalPrice)
                    .surcharge(surcharge)
                    .status(status)
                    .statusLabel(statusLabel)
                    .canSelect(canSelect)
                    .build();

            if (s.getFloorNo() == 1) {
                floor1.add(dto);
            } else {
                floor2.add(dto);
            }
        }

        // Tinh so luong ghe chiem cho
        long occupiedCount = bookedSeatIds.size() + heldMap.size();
        BusTripResponse tripResp = BusTripResponse.from(trip, occupiedCount);

        return TripSeatMapResponse.builder()
                .trip(tripResp)
                .floor1Seats(floor1)
                .floor2Seats(floor2)
                .totalFloors(trip.getBus().getFloors())
                .basePrice(basePrice)
                .vipSurcharge(vipSurcharge)
                .holdMinutes(holdMinutes)
                .currentHoldExpiresAt(earliestExpiry)
                .myHeldSeatIds(myHeldSeatIds)
                .build();
    }

    @Override
    @Transactional
    public List<SeatHold> holdSeats(Long tripId, List<Long> seatIds, Long userId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một ghế");
        }

        BusTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", tripId));

        if (!trip.isBookable()) {
            throw new BusinessException("Chuyến xe này không còn nhận đặt vé hoặc đã khởi hành");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new BusinessException("Một số ghế được chọn không tồn tại trên hệ thống");
        }

        // Kiem tra ghe phai thuoc ve xe cua chuyen
        Long busId = trip.getBus().getId();
        for (Seat s : seats) {
            if (!s.getBus().getId().equals(busId)) {
                throw new BusinessException("Ghế " + s.getSeatCode() + " không thuộc chuyến xe này");
            }
        }

        // Kiem tra xem co ghe nao da ban khong
        Set<Long> bookedSeatIds = ticketDetailRepository.findBookedSeatIds(tripId);
        for (Seat s : seats) {
            if (bookedSeatIds.contains(s.getId())) {
                throw new BusinessException("Ghế " + s.getSeatCode() + " đã có người đặt mua");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // Kiem tra xem co ghe nao dang bi nguoi khac giu khong
        if (seatHoldRepository.existsHeldByOthers(tripId, seatIds, now, userId)) {
            throw new BusinessException("Một số ghế bạn chọn vừa có khách hàng khác giữ chỗ. Vui lòng chọn lại!");
        }

        // Giai phong cac ghe cu user nay dang giu cua chuyen xe nay
        seatHoldRepository.releaseUserHolds(tripId, userId);

        LocalDateTime expiresAt = now.plusMinutes(holdMinutes);
        List<SeatHold> holds = new ArrayList<>();
        for (Seat s : seats) {
            SeatHold h = new SeatHold();
            h.setTrip(trip);
            h.setSeat(s);
            h.setUser(user);
            h.setHeldAt(now);
            h.setExpiresAt(expiresAt);
            h.setStatus(HoldStatus.HELD);
            holds.add(h);
        }

        try {
            return seatHoldRepository.saveAllAndFlush(holds);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Xung dot giu ghe dong thoi tai chuyen {}: {}", tripId, ex.getMessage());
            throw new BusinessException("Ghế bạn chọn vừa được khách hàng khác thao tác giữ chỗ. Vui lòng chọn ghế khác!");
        }
    }

    @Override
    @Transactional
    public void releaseUserHolds(Long tripId, Long userId) {
        seatHoldRepository.releaseUserHolds(tripId, userId);
    }

    @Override
    @Transactional
    public int cleanupExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        int expiredHolds = seatHoldRepository.expireOutdatedHolds(now);

        // Don dep cac ve PENDING qua han giu cho (10 phut)
        LocalDateTime threshold = now.minusMinutes(holdMinutes);
        List<Ticket> expiredTickets = ticketRepository.findByStatusAndCreatedAtBefore(TicketStatus.PENDING, threshold);
        for (Ticket t : expiredTickets) {
            t.setStatus(TicketStatus.EXPIRED);
            t.setCancelReason("Hết thời gian giữ chỗ thanh toán (" + holdMinutes + " phút)");
            for (TicketDetail d : t.getDetails()) {
                d.setStatus(TicketDetailStatus.CANCELLED);
            }
        }
        if (!expiredTickets.isEmpty()) {
            ticketRepository.saveAll(expiredTickets);
            log.info("Da huy {} ve PENDING het han giu cho", expiredTickets.size());
        }

        if (expiredHolds > 0) {
            log.info("Da giai phong {} ghe giu tam het han", expiredHolds);
        }
        return expiredHolds;
    }
}
