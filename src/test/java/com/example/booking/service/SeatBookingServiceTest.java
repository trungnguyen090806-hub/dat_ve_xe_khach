package com.example.booking.service;

import com.example.booking.exception.BusinessException;
import com.example.booking.model.Bus;
import com.example.booking.model.BusTrip;
import com.example.booking.model.Seat;
import com.example.booking.model.SeatHold;
import com.example.booking.model.User;
import com.example.booking.model.enums.TripStatus;
import com.example.booking.repository.BusTripRepository;
import com.example.booking.repository.SeatHoldRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.TicketDetailRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.impl.SeatBookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatBookingServiceTest {

    @Mock
    private BusTripRepository tripRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private TicketDetailRepository ticketDetailRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SeatBookingServiceImpl seatBookingService;

    private BusTrip trip;
    private Bus bus;
    private Seat seatA01;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatBookingService, "holdMinutes", 10);

        bus = new Bus();
        bus.setId(1L);

        trip = new BusTrip();
        trip.setId(10L);
        trip.setBus(bus);
        trip.setStatus(TripStatus.SCHEDULED);
        trip.setDepartureTime(LocalDateTime.now().plusDays(1));

        seatA01 = new Seat();
        seatA01.setId(101L);
        seatA01.setSeatCode("A01");
        seatA01.setBus(bus);

        user = new User();
        user.setId(5L);
        user.setUsername("customer1");
    }

    @Test
    @DisplayName("Chọn ghế còn trống thành công -> Giữ chỗ 10 phút")
    void testHoldSeatSuccess() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(seatRepository.findAllById(List.of(101L))).thenReturn(List.of(seatA01));
        when(ticketDetailRepository.findBookedSeatIds(10L)).thenReturn(Set.of());
        when(seatHoldRepository.existsHeldByOthers(anyLong(), anyList(), any(), anyLong())).thenReturn(false);
        when(seatHoldRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SeatHold> holds = seatBookingService.holdSeats(10L, List.of(101L), 5L);

        assertNotNull(holds);
        assertEquals(1, holds.size());
        assertEquals(seatA01, holds.get(0).getSeat());
        assertEquals(user, holds.get(0).getUser());
        verify(seatHoldRepository, times(1)).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("Chọn ghế đã được đặt -> Ném BusinessException")
    void testHoldAlreadyBookedSeat() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(seatRepository.findAllById(List.of(101L))).thenReturn(List.of(seatA01));
        // Ghế A01 đã được đặt trong ticketDetailRepository
        when(ticketDetailRepository.findBookedSeatIds(10L)).thenReturn(Set.of(101L));

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            seatBookingService.holdSeats(10L, List.of(101L), 5L);
        });

        assertTrue(ex.getMessage().contains("đã có người đặt mua"));
        verify(seatHoldRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("Hai người cùng đặt một ghế (Xung đột đồng thời DB) -> Ném lỗi thân thiện")
    void testConcurrentHoldSameSeat() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(seatRepository.findAllById(List.of(101L))).thenReturn(List.of(seatA01));
        when(ticketDetailRepository.findBookedSeatIds(10L)).thenReturn(Set.of());
        when(seatHoldRepository.existsHeldByOthers(anyLong(), anyList(), any(), anyLong())).thenReturn(false);

        // Gia lap MySQL nem DataIntegrityViolationException do trung Unique index uk_hold_trip_seat_active
        when(seatHoldRepository.saveAllAndFlush(anyList()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key uk_hold_trip_seat_active"));

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            seatBookingService.holdSeats(10L, List.of(101L), 5L);
        });

        assertTrue(ex.getMessage().contains("vừa được khách hàng khác thao tác giữ chỗ"));
    }

    @Test
    @DisplayName("Không cho phép đặt ghế chuyến xe đã khởi hành trong quá khứ")
    void testHoldSeatForDepartedTrip() {
        trip.setDepartureTime(LocalDateTime.now().minusHours(1));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            seatBookingService.holdSeats(10L, List.of(101L), 5L);
        });

        assertTrue(ex.getMessage().contains("không còn nhận đặt vé hoặc đã khởi hành"));
        verify(seatHoldRepository, never()).saveAllAndFlush(anyList());
    }
}
