package com.example.booking.controller.admin;

import com.example.booking.model.Promotion;
import com.example.booking.model.enums.DiscountType;
import com.example.booking.repository.PromotionRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/promotions")
public class AdminPromotionController extends AdminBaseController {

    private final PromotionRepository promotionRepository;

    public AdminPromotionController(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @GetMapping
    public String list(Model model) {
        List<Promotion> promotions = promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("promotions", promotions);
        model.addAttribute("discountTypes", DiscountType.values());
        return "admin/promotions";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Promotion promotion, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("toastError", validationMessage(result));
            return "redirect:/admin/promotions";
        }

        return execute(ra, "/admin/promotions", "Đã lưu mã khuyến mãi thành công", () -> {
            promotion.setCode(promotion.getCode().trim().toUpperCase());
            promotionRepository.save(promotion);
        });
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes ra) {
        return execute(ra, "/admin/promotions", "Đã cập nhật trạng thái khuyến mãi", () -> {
            Promotion p = promotionRepository.findById(id).orElseThrow();
            p.setActive(!p.isActive());
            promotionRepository.save(p);
        });
    }
}
