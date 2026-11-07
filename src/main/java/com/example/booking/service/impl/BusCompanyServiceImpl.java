package com.example.booking.service.impl;

import com.example.booking.dto.BusCompanyRequest;
import com.example.booking.dto.BusCompanyResponse;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.BusCompany;
import com.example.booking.model.enums.CompanyStatus;
import com.example.booking.repository.BusCompanyRepository;
import com.example.booking.repository.BusRepository;
import com.example.booking.repository.BusTripRepository;
import com.example.booking.service.BusCompanyService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BusCompanyServiceImpl implements BusCompanyService {

    private final BusCompanyRepository companyRepository;
    private final BusRepository busRepository;
    private final BusTripRepository tripRepository;

    public BusCompanyServiceImpl(BusCompanyRepository companyRepository, BusRepository busRepository,
                                 BusTripRepository tripRepository) {
        this.companyRepository = companyRepository;
        this.busRepository = busRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BusCompanyResponse> search(String keyword, CompanyStatus status, int page, int size) {
        Specification<BusCompany> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("hotline")), like),
                        cb.like(cb.lower(root.get("address")), like)));
            }
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return companyRepository.findAll(spec, PageRequest.of(page, size, Sort.by("name")))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BusCompanyResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public BusCompanyResponse save(BusCompanyRequest r) {
        BusCompany company;
        String name = r.getName().trim();
        if (r.getId() == null) {
            if (companyRepository.existsByNameIgnoreCase(name)) {
                throw new BusinessException("Tên nhà xe đã tồn tại");
            }
            company = new BusCompany();
        } else {
            company = find(r.getId());
            if (companyRepository.existsByNameIgnoreCaseAndIdNot(name, r.getId())) {
                throw new BusinessException("Tên nhà xe đã tồn tại");
            }
        }
        company.setName(name);
        company.setHotline(r.getHotline().trim());
        company.setEmail(blankToNull(r.getEmail()));
        company.setAddress(r.getAddress().trim());
        company.setDescription(blankToNull(r.getDescription()));
        company.setStatus(r.getStatus());
        return toResponse(companyRepository.save(company));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BusCompany company = find(id);
        if (busRepository.existsByCompanyId(id)) {
            throw new BusinessException("Nhà xe còn xe đang quản lý, không thể xóa. Hãy xóa xe trước hoặc chuyển sang 'Ngừng hoạt động'.");
        }
        companyRepository.delete(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusCompanyResponse> getFeaturedCompanies() {
        return companyRepository.findTop6ByStatusOrderByRatingAvgDesc(CompanyStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusCompanyResponse> getActiveCompanies() {
        return companyRepository.findByStatusOrderByNameAsc(CompanyStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    private BusCompany find(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Nhà xe", id));
    }

    private BusCompanyResponse toResponse(BusCompany c) {
        return BusCompanyResponse.from(c, busRepository.countByCompanyId(c.getId()),
                tripRepository.countByBusCompanyId(c.getId()));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
