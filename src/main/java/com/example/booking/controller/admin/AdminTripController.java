package com.example.booking.controller.admin;

import com.example.booking.dto.BusTripRequest;
import com.example.booking.model.enums.TripStatus;
import com.example.booking.service.BusService;
import com.example.booking.service.BusTripService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/trips")
public class AdminTripController extends AdminBaseController {

    private final BusTripService tripService;
    private final BusService busService;

    public AdminTripController(BusTripService tripService, BusService busService) {
        this.tripService = tripService;
        this.busService = busService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) TripStatus status,
                       Model model) {
        model.addAttribute("page", tripService.search(keyword, status, Math.max(page, 0), PAGE_SIZE));
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusParam", statusParam(status));
        model.addAttribute("statuses", TripStatus.values());
        model.addAttribute("buses", busService.findAssignable());
        return "admin/trips";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute BusTripRequest request, BindingResult result,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("toastError", validationMessage(result));
            return "redirect:/admin/trips";
        }
        boolean creating = request.getId() == null;
        return execute(ra, "/admin/trips",
                creating ? "Đã thêm chuyến xe" : "Đã cập nhật chuyến xe",
                () -> tripService.save(request));
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        return execute(ra, "/admin/trips", "Đã hủy chuyến xe và hoàn tiền các vé liên quan",
                () -> tripService.cancel(id));
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        return execute(ra, "/admin/trips", "Đã xóa chuyến xe", () -> tripService.delete(id));
    }
}
