package com.example.booking.service.impl;

import com.example.booking.dto.BookingCheckoutForm;
import com.example.booking.dto.BookingCheckoutView;
import com.example.booking.dto.BusTripResponse;
import com.example.booking.dto.SeatDto;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.BusTrip;
import com.example.booking.model.Notification;
import com.example.booking.model.Payment;
import com.example.booking.model.Promotion;
import com.example.booking.model.Seat;
import com.example.booking.model.SeatHold;
import com.example.booking.model.Ticket;
import com.example.booking.model.TicketDetail;
import com.example.booking.model.User;
import com.example.booking.model.enums.HoldStatus;
import com.example.booking.model.enums.MembershipType;
import com.example.booking.model.enums.NotificationType;
import com.example.booking.model.enums.PaymentStatus;
import com.example.booking.model.enums.SeatType;
import com.example.booking.model.enums.TicketDetailStatus;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.repository.BusTripRepository;
import com.example.booking.repository.NotificationRepository;
import com.example.booking.repository.PaymentRepository;
import com.example.booking.repository.PromotionRepository;
import com.example.booking.repository.SeatHoldRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.TicketDetailRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.BookingService;
import com.example.booking.service.pricing.PricingStrategy;
import com.example.booking.utils.CodeGenerator;
import com.example.booking.utils.QrCodeGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BookingServiceImpl implements BookingService {

    private final TicketRepository ticketRepository;
    private final TicketDetailRepository ticketDetailRepository;
    private final PaymentRepository paymentRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BusTripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PromotionRepository promotionRepository;
    private final NotificationRepository notificationRepository;
    private final PricingStrategy pricingStrategy;

    public BookingServiceImpl(TicketRepository ticketRepository,
                              TicketDetailRepository ticketDetailRepository,
                              PaymentRepository paymentRepository,
                              SeatHoldRepository seatHoldRepository,
                              BusTripRepository tripRepository,
                              SeatRepository seatRepository,
                              UserRepository userRepository,
                              PromotionRepository promotionRepository,
                              NotificationRepository notificationRepository,
                              PricingStrategy pricingStrategy) {
        this.ticketRepository = ticketRepository;
        this.ticketDetailRepository = ticketDetailRepository;
        this.paymentRepository = paymentRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.tripRepository = tripRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.promotionRepository = promotionRepository;
        this.notificationRepository = notificationRepository;
        this.pricingStrategy = pricingStrategy;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingCheckoutView prepareCheckout(Long tripId, List<Long> seatIds, String promoCode, Long userId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessException("Danh sách ghế không được để trống");
        }

        BusTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", tripId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new BusinessException("Một số ghế không tồn tại");
        }

        BigDecimal basePrice = trip.getBasePrice();
        BigDecimal vipSurcharge = trip.getVipSurcharge();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal vipSurchargeTotal = BigDecimal.ZERO;
        List<SeatDto> seatDtos = new ArrayList<>();

        for (Seat s : seats) {
            subtotal = subtotal.add(basePrice);
            BigDecimal surcharge = (s.getSeatType() == SeatType.VIP) ? vipSurcharge : BigDecimal.ZERO;
            vipSurchargeTotal = vipSurchargeTotal.add(surcharge);

            seatDtos.add(SeatDto.builder()
                    .id(s.getId())
                    .seatCode(s.getSeatCode())
                    .floorNo(s.getFloorNo())
                    .seatType(s.getSeatType().name())
                    .seatTypeLabel(s.getSeatType().getLabel())
                    .price(basePrice.add(surcharge))
                    .surcharge(surcharge)
                    .status("SELECTED")
                    .statusLabel("Đang chọn")
                    .canSelect(true)
                    .build());
        }

        BigDecimal subtotalWithVip = subtotal.add(vipSurchargeTotal);
        MembershipType memType = user.getMembershipType();
        BigDecimal memberDiscount = pricingStrategy.calculateMemberDiscount(memType, subtotalWithVip);

        // Kiem tra khuyen mai
        BigDecimal promoDiscount = BigDecimal.ZERO;
        String promoMessage = null;

        if (promoCode != null && !promoCode.isBlank()) {
            String cleanCode = promoCode.trim().toUpperCase();
            Optional<Promotion> promoOpt = promotionRepository.findByCodeIgnoreCaseAndActiveTrue(cleanCode);
            if (promoOpt.isPresent()) {
                Promotion promo = promoOpt.get();
                LocalDate today = LocalDate.now();
                if (promo.isUsableOn(today, subtotalWithVip)) {
                    promoDiscount = pricingStrategy.calculatePromotionDiscount(
                            subtotalWithVip, promo.getDiscountValue(),
                            promo.getDiscountType().name(), promo.getMaxDiscount());
                    promoMessage = "Áp dụng mã " + promo.getCode() + " thành công (-" + formatVnd(promoDiscount) + ")";
                } else {
                    promoMessage = "Mã khuyến mãi không khả dụng hoặc đơn chưa đạt mức tối thiểu " + formatVnd(promo.getMinOrder());
                }
            } else {
                promoMessage = "Mã khuyến mãi không tồn tại hoặc đã hết hiệu lực";
            }
        }

        BigDecimal totalAmount = subtotalWithVip.subtract(memberDiscount).subtract(promoDiscount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        long occupied = ticketDetailRepository.countByTripIdAndStatus(trip.getId(), TicketDetailStatus.ACTIVE);
        BusTripResponse tripResp = BusTripResponse.from(trip, occupied);

        return BookingCheckoutView.builder()
                .trip(tripResp)
                .seats(seatDtos)
                .defaultPassengerName(user.getFullName())
                .defaultPassengerPhone(user.getPhone())
                .defaultPassengerEmail(user.getEmail())
                .subtotal(subtotal)
                .vipSurchargeTotal(vipSurchargeTotal)
                .membershipType(memType)
                .memberDiscountRate(memType.getDiscountRate())
                .memberDiscount(memberDiscount)
                .promoCode(promoCode)
                .promoDiscount(promoDiscount)
                .promoMessage(promoMessage)
                .totalAmount(totalAmount)
                .build();
    }

    @Override
    @Transactional
    public Ticket processBooking(BookingCheckoutForm form, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));

        BusTrip trip = tripRepository.findById(form.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", form.getTripId()));

        if (!trip.isBookable()) {
            throw new BusinessException("Chuyến xe không còn nhận đặt vé hoặc đã khởi hành");
        }

        List<Seat> seats = seatRepository.findAllById(form.getSeatIds());
        if (seats.size() != form.getSeatIds().size()) {
            throw new BusinessException("Một số ghế không tồn tại trên hệ thống");
        }

        // Kiem tra ghe khong duoc nam trong ve ACTIVE
        Set<Long> bookedSeatIds = ticketDetailRepository.findBookedSeatIds(trip.getId());
        for (Seat s : seats) {
            if (bookedSeatIds.contains(s.getId())) {
                throw new BusinessException("Ghế " + s.getSeatCode() + " đã có khách hàng khác đặt mua");
            }
        }

        // Tinh toan gia
        BigDecimal basePrice = trip.getBasePrice();
        BigDecimal vipSurcharge = trip.getVipSurcharge();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal vipSurchargeTotal = BigDecimal.ZERO;

        for (Seat s : seats) {
            subtotal = subtotal.add(basePrice);
            if (s.getSeatType() == SeatType.VIP) {
                vipSurchargeTotal = vipSurchargeTotal.add(vipSurcharge);
            }
        }

        BigDecimal subtotalWithVip = subtotal.add(vipSurchargeTotal);
        BigDecimal memberDiscount = pricingStrategy.calculateMemberDiscount(user.getMembershipType(), subtotalWithVip);

        // Khuyen mai
        BigDecimal promoDiscount = BigDecimal.ZERO;
        Promotion appliedPromo = null;

        if (form.getPromoCode() != null && !form.getPromoCode().isBlank()) {
            String cleanCode = form.getPromoCode().trim().toUpperCase();
            Optional<Promotion> promoOpt = promotionRepository.findByCodeIgnoreCaseAndActiveTrue(cleanCode);
            if (promoOpt.isPresent()) {
                Promotion promo = promoOpt.get();
                if (promo.isUsableOn(LocalDate.now(), subtotalWithVip)) {
                    appliedPromo = promo;
                    promoDiscount = pricingStrategy.calculatePromotionDiscount(
                            subtotalWithVip, promo.getDiscountValue(),
                            promo.getDiscountType().name(), promo.getMaxDiscount());
                    promo.setUsedCount(promo.getUsedCount() + 1);
                    promotionRepository.save(promo);
                }
            }
        }

        BigDecimal totalAmount = subtotalWithVip.subtract(memberDiscount).subtract(promoDiscount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // Sinh ma ve duy nhat
        String ticketCode = generateUniqueTicketCode();

        Ticket ticket = new Ticket();
        ticket.setTicketCode(ticketCode);
        ticket.setUser(user);
        ticket.setTrip(trip);
        ticket.setPromotion(appliedPromo);
        ticket.setPassengerName(form.getPassengerName().trim());
        ticket.setPassengerPhone(form.getPassengerPhone().trim());
        ticket.setPassengerEmail(form.getPassengerEmail() != null ? form.getPassengerEmail().trim() : null);
        ticket.setSubtotal(subtotal);
        ticket.setVipSurchargeTotal(vipSurchargeTotal);
        ticket.setMemberDiscount(memberDiscount);
        ticket.setPromoDiscount(promoDiscount);
        ticket.setTotalAmount(totalAmount);
        ticket.setStatus(TicketStatus.PAID); // Moi truong Demo: xac nhan thanh toan thanh cong

        for (Seat s : seats) {
            TicketDetail detail = new TicketDetail();
            detail.setTrip(trip);
            detail.setSeat(s);
            detail.setSeatCode(s.getSeatCode());
            detail.setSeatType(s.getSeatType());
            detail.setPrice(basePrice);
            detail.setSurcharge(s.getSeatType() == SeatType.VIP ? vipSurcharge : BigDecimal.ZERO);
            detail.setStatus(TicketDetailStatus.ACTIVE);
            ticket.addDetail(detail);
        }

        try {
            ticket = ticketRepository.save(ticket);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Rất tiếc! Một số ghế bạn chọn vừa có người đặt thành công trước. Vui lòng chọn ghế khác.");
        }

        // Cap nhat cac ban ghi SeatHold thanh CONFIRMED
        List<SeatHold> userHolds = seatHoldRepository.findByTripIdAndUserIdAndStatusAndExpiresAtAfter(
                trip.getId(), user.getId(), HoldStatus.HELD, LocalDateTime.now());
        for (SeatHold h : userHolds) {
            h.setStatus(HoldStatus.CONFIRMED);
        }
        if (!userHolds.isEmpty()) {
            seatHoldRepository.saveAll(userHolds);
        }

        // Luu lich su thanh toan (Payment)
        Payment payment = new Payment();
        payment.setTicket(ticket);
        payment.setTransactionCode(generateUniqueTransactionCode());
        payment.setMethod(form.getPaymentMethod());
        payment.setAmount(totalAmount);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setNote("Thanh toán thành công vé " + ticketCode + " qua " + form.getPaymentMethod().getLabel() + " (DEMO)");
        paymentRepository.save(payment);

        // Cap nhat tong chi tieu va xet nang hang thanh vien
        updateUserMembership(user, totalAmount);

        // Gui thong bao he thong
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setTitle("Đặt vé thành công - Mã vé: " + ticketCode);
        notif.setContent("Quý khách đã đặt thành công chuyến " + trip.getOrigin() + " đi " + trip.getDestination()
                + ", khởi hành lúc " + trip.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                + ". Tổng thanh toán: " + formatVnd(totalAmount));
        notif.setType(NotificationType.PAYMENT);
        notif.setRead(false);
        notificationRepository.save(notif);

        return ticket;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketByCode(String ticketCode, Long userId, boolean isAdmin) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vé xe", ticketCode));

        // Kiem tra quyen: chi chu so huu hoac Admin moi duoc xem chi tiet ve
        if (!isAdmin && (userId == null || !ticket.getUser().getId().equals(userId))) {
            throw new BusinessException("Bạn không có quyền xem thông tin vé này");
        }
        return ticket;
    }

    @Override
    public String generateTicketQrBase64(Ticket ticket) {
        String content = "BUSGO-TICKET|" + ticket.getTicketCode() + "|"
                + ticket.getPassengerPhone() + "|"
                + ticket.getTrip().getTripCode() + "|"
                + ticket.getTotalAmount();
        return QrCodeGenerator.generateBase64Qr(content, 220, 220);
    }

    private void updateUserMembership(User user, BigDecimal amountSpent) {
        BigDecimal newTotal = user.getTotalSpent().add(amountSpent);
        user.setTotalSpent(newTotal);

        if (newTotal.compareTo(new BigDecimal("10000000")) >= 0) {
            user.setMembershipType(MembershipType.VIP);
        } else if (newTotal.compareTo(new BigDecimal("2000000")) >= 0) {
            if (user.getMembershipType() == MembershipType.REGULAR) {
                user.setMembershipType(MembershipType.MEMBER);
            }
        }
        userRepository.save(user);
    }

    private String generateUniqueTicketCode() {
        String code;
        do {
            code = CodeGenerator.generate("BG", 8);
        } while (ticketRepository.existsByTicketCodeIgnoreCase(code));
        return code;
    }

    private String generateUniqueTransactionCode() {
        return CodeGenerator.generate("TXN", 10);
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0đ";
        return new java.text.DecimalFormat("#,###đ").format(amount);
    }
}
