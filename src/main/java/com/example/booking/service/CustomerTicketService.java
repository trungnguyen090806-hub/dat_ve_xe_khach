package com.example.booking.service;

import com.example.booking.model.Ticket;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.service.policy.CancellationPolicy;
import org.springframework.data.domain.Page;

public interface CustomerTicketService {

    Page<Ticket> getMyTickets(Long userId, TicketStatus status, int page, int size);

    Ticket getTicketDetail(Long ticketId, Long userId);

    Ticket lookupTicket(String ticketCode, String phone);

    void cancelTicket(Long ticketId, String reason, Long userId);

    CancellationPolicy getCancellationPolicy();
}
