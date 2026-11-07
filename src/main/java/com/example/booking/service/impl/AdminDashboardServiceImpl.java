package com.example.booking.service.impl;

import com.example.booking.dto.AdminDashboardStats;
import com.example.booking.model.Ticket;
import com.example.booking.model.enums.TicketDetailStatus;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.repository.BusTripRepository;
import com.example.booking.repository.PaymentRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.TicketDetailRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final BusTripRepository tripRepository;
    private final UserRepository userRepository;
    private final TicketDetailRepository ticketDetailRepository;
    private final SeatRepository seatRepository;

    public AdminDashboardServiceImpl(PaymentRepository paymentRepository,
                                     TicketRepository ticketRepository,
                                     BusTripRepository tripRepository,
                                     UserRepository userRepository,
                                     TicketDetailRepository ticketDetailRepository,
                                     SeatRepository seatRepository) {
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.ticketDetailRepository = ticketDetailRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        BigDecimal totalRevenue = paymentRepository.sumTotalSuccessfulAmount();
        long totalTrips = tripRepository.count();
        long totalSoldTickets = ticketRepository.countByStatusIn(List.of(TicketStatus.PAID, TicketStatus.COMPLETED));
        long totalCustomers = userRepository.count();
        long pendingTickets = ticketRepository.countByStatus(TicketStatus.PENDING);

        long totalSeats = seatRepository.count();
        long bookedSeats = ticketDetailRepository.countByStatus(TicketDetailStatus.ACTIVE);
        double occupancyRate = totalSeats > 0 ? (bookedSeats * 100.0) / totalSeats : 0.0;

        // 1. Doanh thu 6 thang gan nhat
        List<String> monthlyLabels = new ArrayList<>();
        List<BigDecimal> monthlyData = new ArrayList<>();
        YearMonth currentYm = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentYm.minusMonths(i);
            monthlyLabels.add("Tháng " + ym.getMonthValue() + "/" + ym.getYear());
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
            BigDecimal rev = paymentRepository.sumRevenueBetween(start, end);
            monthlyData.add(rev != null ? rev : BigDecimal.ZERO);
        }

        // 2. So ve ban theo 7 ngay gan nhat
        List<String> dailyLabels = new ArrayList<>();
        List<Long> dailyData = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dailyLabels.add(d.format(DateTimeFormatter.ofPattern("dd/MM")));
            LocalDateTime start = d.atStartOfDay();
            LocalDateTime end = d.plusDays(1).atStartOfDay();
            long count = ticketRepository.countByCreatedAtBetweenAndStatusIn(
                    start, end, List.of(TicketStatus.PAID, TicketStatus.COMPLETED));
            dailyData.add(count);
        }

        // 3. Doanh thu theo tuyen
        List<Object[]> routeRevenues = ticketRepository.sumRevenueByRoute();
        List<String> routeLabels = new ArrayList<>();
        List<BigDecimal> routeData = new ArrayList<>();

        int limit = Math.min(5, routeRevenues.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = routeRevenues.get(i);
            routeLabels.add((String) row[0]);
            routeData.add((BigDecimal) row[1]);
        }

        return AdminDashboardStats.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalTrips(totalTrips)
                .totalSoldTickets(totalSoldTickets)
                .totalCustomers(totalCustomers)
                .pendingTickets(pendingTickets)
                .occupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                .monthlyRevenueLabels(monthlyLabels)
                .monthlyRevenueData(monthlyData)
                .dailyTicketsLabels(dailyLabels)
                .dailyTicketsData(dailyData)
                .routeRevenueLabels(routeLabels)
                .routeRevenueData(routeData)
                .totalSeatsInSystem(totalSeats)
                .totalBookedSeats(bookedSeats)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void exportRevenueExcel(HttpServletResponse response) throws IOException {
        AdminDashboardStats stats = getDashboardStats();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo Doanh thu BusGo");

            // Fonts & Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            int rowIdx = 0;

            // Row 0: Tieu de
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HỆ THỐNG ĐẶT VÉ XE KHÁCH BUSGO - BÁO CÁO DOANH THU");
            titleCell.setCellStyle(titleStyle);

            // Row 1: Thoi gian xuat
            Row dateRow = sheet.createRow(rowIdx++);
            dateRow.createCell(0).setCellValue("Thời gian xuất báo cáo: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")));

            rowIdx++; // cach 1 dong

            // BẢNG 1: TỔNG QUAN HỆ THỐNG
            Row sec1Row = sheet.createRow(rowIdx++);
            sec1Row.createCell(0).setCellValue("1. SỐ LIỆU TỔNG QUAN");

            Row tblHeader1 = sheet.createRow(rowIdx++);
            String[] headers1 = {"Chỉ số", "Giá trị"};
            for (int i = 0; i < headers1.length; i++) {
                Cell c = tblHeader1.createCell(i);
                c.setCellValue(headers1[i]);
                c.setCellStyle(headerStyle);
            }

            Object[][] summaryData = {
                    {"Tổng doanh thu thực tế (VNĐ)", stats.getTotalRevenue() + " đ"},
                    {"Tổng số chuyến xe đã khởi tạo", stats.getTotalTrips()},
                    {"Tổng số vé đã bán thành công", stats.getTotalSoldTickets()},
                    {"Số vé chờ thanh toán (Pending)", stats.getPendingTickets()},
                    {"Tổng số tài khoản người dùng", stats.getTotalCustomers()},
                    {"Tỷ lệ lấp đầy ghế", stats.getOccupancyRate() + " %"}
            };

            for (Object[] row : summaryData) {
                Row r = sheet.createRow(rowIdx++);
                Cell c0 = r.createCell(0);
                c0.setCellValue(row[0].toString());
                c0.setCellStyle(borderStyle);

                Cell c1 = r.createCell(1);
                c1.setCellValue(row[1].toString());
                c1.setCellStyle(borderStyle);
            }

            rowIdx += 2;

            // BẢNG 2: DOANH THU THEO TUYẾN
            Row sec2Row = sheet.createRow(rowIdx++);
            sec2Row.createCell(0).setCellValue("2. DOANH THU THEO TUYẾN ĐƯỜNG");

            Row tblHeader2 = sheet.createRow(rowIdx++);
            Cell h2_0 = tblHeader2.createCell(0);
            h2_0.setCellValue("Tuyến đường");
            h2_0.setCellStyle(headerStyle);

            Cell h2_1 = tblHeader2.createCell(1);
            h2_1.setCellValue("Doanh thu (VNĐ)");
            h2_1.setCellStyle(headerStyle);

            for (int i = 0; i < stats.getRouteRevenueLabels().size(); i++) {
                Row r = sheet.createRow(rowIdx++);
                Cell c0 = r.createCell(0);
                c0.setCellValue(stats.getRouteRevenueLabels().get(i));
                c0.setCellStyle(borderStyle);

                Cell c1 = r.createCell(1);
                c1.setCellValue(stats.getRouteRevenueData().get(i).doubleValue());
                c1.setCellStyle(borderStyle);
            }

            rowIdx += 2;

            // BẢNG 3: VÉ ĐẶT GẦN ĐÂY
            Row sec3Row = sheet.createRow(rowIdx++);
            sec3Row.createCell(0).setCellValue("3. DANH SÁCH VÉ BÁN GẦN ĐÂY");

            Row tblHeader3 = sheet.createRow(rowIdx++);
            String[] headers3 = {"Mã vé", "Hành khách", "Số điện thoại", "Tuyến đường", "Khởi hành", "Tổng tiền (VNĐ)", "Trạng thái"};
            for (int i = 0; i < headers3.length; i++) {
                Cell c = tblHeader3.createCell(i);
                c.setCellValue(headers3[i]);
                c.setCellStyle(headerStyle);
            }

            List<Ticket> recentTickets = ticketRepository.findAll(
                    PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

            for (Ticket t : recentTickets) {
                Row r = sheet.createRow(rowIdx++);
                Cell c0 = r.createCell(0); c0.setCellValue(t.getTicketCode()); c0.setCellStyle(borderStyle);
                Cell c1 = r.createCell(1); c1.setCellValue(t.getPassengerName()); c1.setCellStyle(borderStyle);
                Cell c2 = r.createCell(2); c2.setCellValue(t.getPassengerPhone()); c2.setCellStyle(borderStyle);
                Cell c3 = r.createCell(3); c3.setCellValue(t.getTrip().getOrigin() + " - " + t.getTrip().getDestination()); c3.setCellStyle(borderStyle);
                Cell c4 = r.createCell(4); c4.setCellValue(t.getTrip().getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))); c4.setCellStyle(borderStyle);
                Cell c5 = r.createCell(5); c5.setCellValue(t.getTotalAmount().doubleValue()); c5.setCellStyle(borderStyle);
                Cell c6 = r.createCell(6); c6.setCellValue(t.getStatus().getLabel()); c6.setCellStyle(borderStyle);
            }

            // Auto-size columns
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            String filename = "Bao_cao_doanh_thu_BusGo_" + LocalDate.now().toString() + ".xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);

            workbook.write(response.getOutputStream());
            response.flushBuffer();
        }
    }
}
