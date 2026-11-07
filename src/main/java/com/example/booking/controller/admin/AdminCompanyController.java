package com.example.booking.controller.admin;

import com.example.booking.dto.BusRequest;
import com.example.booking.dto.BusCompanyRequest;
import com.example.booking.model.enums.BusType;
import com.example.booking.model.enums.CompanyStatus;
import com.example.booking.service.BusCompanyService;
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
@RequestMapping("/admin/companies")
public class AdminCompanyController extends AdminBaseController {

    private final BusCompanyService companyService;
    private final BusService busService;
    private final BusTripService tripService;

    public AdminCompanyController(BusCompanyService companyService, BusService busService,
                                  BusTripService tripService) {
        this.companyService = companyService;
        this.busService = busService;
        this.tripService = tripService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) CompanyStatus status,
                       Model model) {
        model.addAttribute("page", companyService.search(keyword, status, Math.max(page, 0), PAGE_SIZE));
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusParam", statusParam(status));
        model.addAttribute("statuses", CompanyStatus.values());
        return "admin/companies";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getById(id));
        model.addAttribute("buses", busService.findByCompany(id));
        model.addAttribute("trips", tripService.findByCompany(id));
        model.addAttribute("busTypes", BusType.values());
        return "admin/company-detail";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute BusCompanyRequest request, BindingResult result,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("toastError", validationMessage(result));
            return "redirect:/admin/companies";
        }
        boolean creating = request.getId() == null;
        return execute(ra, "/admin/companies",
                creating ? "Đã thêm nhà xe" : "Đã cập nhật nhà xe",
                () -> companyService.save(request));
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        return execute(ra, "/admin/companies", "Đã xóa nhà xe", () -> companyService.delete(id));
    }

    // ---- Quan ly xe cua nha xe ----

    @PostMapping("/{id}/buses")
    public String addBus(@PathVariable Long id, @Valid @ModelAttribute BusRequest request,
                         BindingResult result, RedirectAttributes ra) {
        String back = "/admin/companies/" + id;
        if (result.hasErrors()) {
            ra.addFlashAttribute("toastError", validationMessage(result));
            return "redirect:" + back;
        }
        request.setCompanyId(id);
        return execute(ra, back, "Đã thêm xe và tạo sơ đồ ghế", () -> busService.create(request));
    }

    @PostMapping("/{id}/buses/{busId}/delete")
    public String deleteBus(@PathVariable Long id, @PathVariable Long busId, RedirectAttributes ra) {
        return execute(ra, "/admin/companies/" + id, "Đã xóa xe", () -> busService.delete(busId));
    }
}
