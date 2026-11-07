package com.example.booking.controller.admin;

import com.example.booking.model.User;
import com.example.booking.model.enums.MembershipType;
import com.example.booking.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController extends AdminBaseController {

    private final UserRepository userRepository;

    public AdminCustomerController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) MembershipType membership,
                       Model model) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("phone")), like)
                ));
            }
            if (membership != null) {
                ps.add(cb.equal(root.get("membershipType"), membership));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, PageRequest.of(
                Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("page", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("membershipParam", membership != null ? membership.name() : "");
        model.addAttribute("membershipTypes", MembershipType.values());

        return "admin/customers";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes ra) {
        return execute(ra, "/admin/customers", "Đã cập nhật trạng thái tài khoản", () -> {
            User user = userRepository.findById(id).orElseThrow();
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
        });
    }

    @PostMapping("/{id}/update-membership")
    public String updateMembership(@PathVariable Long id,
                                   @RequestParam MembershipType membershipType,
                                   RedirectAttributes ra) {
        return execute(ra, "/admin/customers", "Đã cập nhật hạng thành viên", () -> {
            User user = userRepository.findById(id).orElseThrow();
            user.setMembershipType(membershipType);
            userRepository.save(user);
        });
    }
}
