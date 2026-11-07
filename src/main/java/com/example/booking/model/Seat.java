package com.example.booking.model;

import com.example.booking.model.enums.SeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ghe trong so do cua mot xe. Trang thai ghe theo tung chuyen duoc suy ra tu TicketDetail / SeatHold. */
@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
public class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "seat_code", nullable = false, length = 10)
    private String seatCode;

    @Column(name = "floor_no", nullable = false)
    private int floorNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    private SeatType seatType = SeatType.NORMAL;
}
