package com.example.booking.controller.admin;

import com.example.booking.model.Payment;
import com.example.booking.model.enums.PaymentMethod;
import com.example.booking.model.enums.PaymentStatus;
import com.example.booking.repository.PaymentRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController extends AdminBaseController {

    private final PaymentRepository paymentRepository;

    public AdminPaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) PaymentMethod method,
                       @RequestParam(required = false) PaymentStatus status,
                       Model model) {
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("transactionCode")), like),
                        cb.like(cb.lower(root.join("ticket").get("ticketCode")), like)
                ));
            }
            if (method != null) {
                ps.add(cb.equal(root.get("method"), method));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<Payment> paymentPage = paymentRepository.findAll(spec, PageRequest.of(
                Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("page", paymentPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("methodParam", method != null ? method.name() : "");
        model.addAttribute("statusParam", status != null ? status.name() : "");
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("statuses", PaymentStatus.values());

        return "admin/payments";
    }
}
