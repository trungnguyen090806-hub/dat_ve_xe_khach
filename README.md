# HỆ THỐNG QUẢN LÝ ĐẶT VÉ XE KHÁCH TRỰC TUYẾN (BUS TICKET BOOKING MANAGEMENT SYSTEM)

> **Đồ án chuyên ngành / Đồ án tốt nghiệp Công nghệ Thông tin**  
> **Kiến trúc:** Spring Boot 3, Spring Security 6, Spring Data JPA, Thymeleaf, MySQL 8, Bootstrap 5.

---

## 1. Giới thiệu Đề tài

Hệ thống Quản lý và Đặt vé Xe khách Trực tuyến là giải pháp chuyển đổi số toàn diện cho các đơn vị kinh doanh vận tải hành khách đường bộ và khách hàng. Hệ thống hỗ trợ tra cứu chuyến đi, sơ đồ trực quan 2 tầng (xe giường nằm/limousine), giữ chỗ tức thời trong 10 phút, đặt vé, tính giá tự động theo cấp bậc thành viên, thanh toán giả lập với mã QR vé điện tử, tra cứu vé và quản trị vận tải chuyên sâu cho Nhà xe & Quản trị viên (Admin).

### Điểm nổi bật kỹ thuật:
- **Chống đặt trùng ghế tuyệt đối (Zero Double-Booking)**: Kết hợp ràng buộc cấp Cơ sở dữ liệu (**Generated Columns + Partial Unique Index** trong MySQL 8) cùng xử lý giao dịch mức dịch vụ (`@Transactional` và bắt `DataIntegrityViolationException`).
- **Giữ ghế tạm thời (Seat Hold Mechanism)**: Đặt chỗ tạm thời trong **10 phút**, đồng bộ trạng thái thời gian thực và tự động giải phóng ghế hết hạn qua **Spring Task Scheduler (`@Scheduled`)** chạy nền mỗi 60 giây.
- **Hệ thống phân hạng & Giảm giá tự động (Strategy Pattern)**: Khách thường (0%), Thành viên (5%), VIP (10%), kết hợp mã khuyến mãi giảm theo % hoặc số tiền cố định.
- **Chính sách hoàn/hủy vé tự động (Policy Pattern)**: Tự động tính toán tỷ lệ hoàn tiền theo mốc thời gian: hủy trước $\ge 24$h (hoàn 100%), từ 6h đến 24h (hoàn 70%), dưới 6h (không hoàn tiền 0%).
- **Báo cáo & Xuất file chuyên nghiệp**: Xuất báo cáo doanh thu, chuyến đi, vé xe ra file **Excel (Apache POI)** với định dạng tiền tệ và khung lưới chuẩn; biểu đồ thống kê trực quan với **Chart.js**; sinh vé kèm **Mã QR (ZXing)** kiểm soát vé lúc lên xe.

---

## 2. Công nghệ Sử dụng (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Phiên bản | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Ngôn ngữ** | Java | 17 (LTS) | Record, Pattern Matching, Stream API |
| **Framework** | Spring Boot | 3.2.5 | Web, Data JPA, Security, Validation |
| **Bảo mật** | Spring Security | 6.x | Form Login, Remember-Me (14 ngày), BCrypt, RBAC |
| **Cơ sở dữ liệu** | MySQL | 8.0+ | InnoDB, Generated Columns, Full Constraints |
| **Giao diện** | Thymeleaf + HTML5 / CSS3 | 3.x | Template Engine server-side rendering, Layout Dialect |
| **Styling & Icons** | Bootstrap 5.3 + Bootstrap Icons | 5.3.3 / 1.11 | Responsive Mobile/Tablet/Desktop, CSS Custom Variables |
| **Biểu đồ** | Chart.js | 4.4 | Doanh thu 7 ngày, tỷ lệ trạng thái vé, top tuyến đường |
| **Xuất Excel** | Apache POI | 5.2.5 | XSSFWorkbook, CellStyle, DataFormat, Font Styling |
| **Mã QR** | ZXing (Zebra Crossing) | 3.5.3 | QrCodeGenerator xuất Base64 PNG inline |
| **Kiểm thử** | JUnit 5 + Mockito + MockMvc | 5.10.x | Unit Test Service/Strategy/Policy, Integration Test Web |
| **Build Tool** | Apache Maven | 3.8+ / 3.9+ | Quản lý vòng đời và dependencies |

---

## 3. Kiến trúc Hệ thống & Áp dụng Design Patterns

Hệ thống được thiết kế theo mô hình **N-Tier Clean Architecture**:

```
com.example.booking
│
├── config/              # Cấu hình hệ thống (SecurityConfig, WebMvcConfig, AppConfig)
├── controller/          # Lớp điều khiển Web & REST API
│   ├── admin/           # Phân hệ Admin (Dashboard, Trips, Companies, Tickets, Reports...)
│   └── ...              # Phân hệ Khách hàng (Home, Search, Booking, Profile, Ticket...)
├── dto/                 # Data Transfer Objects (Validation annotations, Request/Response)
├── exception/           # Xử lý ngoại lệ tập trung (GlobalExceptionHandler, Custom Exceptions)
├── model/               # JPA Entities kế thừa BaseEntity (Auditing)
│   └── enums/           # 11 Enums định nghĩa trạng thái và kiểu dữ liệu
├── repository/          # Spring Data JPA Repositories (Custom Queries, Specifications)
├── security/            # Triển khai CustomUserDetails, CustomUserDetailsService
├── service/             # Interface Service quy định hợp đồng nghiệp vụ
│   ├── impl/            # Triển khai Service (@Transactional, Business Logic)
│   ├── strategy/        # Strategy Pattern tính giá vé và khuyến mãi
│   └── policy/          # Policy Pattern chính sách hủy vé & hoàn tiền
└── util/                # Tiện ích bổ trợ (QrCodeGenerator, DateUtils, PriceFormatter)
```

### Các Mẫu Thiết Kế (Design Patterns)
1. **Strategy Pattern (`PricingStrategy`)**:
   - `RegularPricingStrategy`: Giảm 0% cho hạng REGULAR.
   - `MemberPricingStrategy`: Giảm 5% cho hạng MEMBER.
   - `VipPricingStrategy`: Giảm 10% cho hạng VIP.
   - `PricingStrategyFactory`: Tự động cấp phát Strategy dựa trên `MembershipType` của người dùng và áp dụng mã Coupon hợp lệ.
2. **Policy Pattern (`CancellationPolicy`)**:
   - Tách biệt logic kinh doanh hoàn tiền ra khỏi Service để dễ dàng kiểm thử và cấu hình tỷ lệ hoàn tiền khi chính sách thay đổi.
3. **Specification Pattern (`TripSpecification`)**:
   - Xây dựng câu truy vấn động linh hoạt đa tiêu chí (Điểm đi, Điểm đến, Ngày đi, Hãng xe, Loại xe, Khoảng giá, Giờ xuất phát) mà không bị lỗi câu lệnh SQL nối chuỗi dài.
4. **Repository Pattern**:
   - Trừu tượng hóa hoàn toàn tầng lưu trữ dữ liệu thông qua Spring Data JPA.

---

## 4. Cơ sở Dữ liệu & Cơ chế Chống Đặt Trùng Ghế

### 4.1. Danh sách 14 Bảng dữ liệu
1. `roles`: Phân quyền người dùng (`ROLE_ADMIN`, `ROLE_CUSTOMER`).
2. `users`: Tài khoản khách hàng và quản trị viên, lưu hạng thành viên và tổng tích lũy chi tiêu.
3. `user_roles`: Bảng liên kết người dùng và quyền hạn (N-N).
4. `bus_companies`: Thông tin nhà xe (Phương Trang, Mai Linh, Thành Bưởi, Hoa Mai...).
5. `buses`: Phương tiện xe khách (Biển số, số chỗ, loại xe: Giường nằm 2 tầng, Limousine...).
6. `seats`: Danh sách ghế/giường của từng xe (Tầng 1: A01..A18, Tầng 2: B01..B18, VIP).
7. `bus_trips`: Chuyến xe (Tuyến đường, ngày giờ đi/đến, giá gốc, trạng thái).
8. `seat_holds`: Quản lý giữ ghế tạm thời trong 10 phút chống xung đột phiên đặt.
9. `tickets`: Thông tin đơn đặt vé tổng thể (Mã vé `TKT-xxxx`, tổng tiền, thanh toán).
10. `ticket_details`: Chi tiết từng vé theo ghế trên chuyến xe cụ thể.
11. `payments`: Giao dịch thanh toán (Mã giao dịch, hình thức: VNPAY, MOMO, Tiền mặt).
12. `promotions`: Mã giảm giá (Coupon code, mức giảm theo % hoặc VNĐ, thời hạn).
13. `reviews`: Đánh giá chất lượng nhà xe và chuyến đi (1 đến 5 sao kèm bình luận).
14. `notifications`: Thông báo hệ thống gửi đến người dùng.

### 4.2. Cơ chế Chống Trùng Ghế (Zero Double-Booking)
Trong MySQL, ràng buộc `UNIQUE` thông thường sẽ coi các bản ghi có trạng thái `CANCELLED` là vi phạm nếu khách hàng muốn đặt lại ghế đó. Hệ thống giải quyết triệt để bằng **Generated Column (`STORED`)**:
```sql
ALTER TABLE ticket_details
ADD COLUMN active_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN status = 'ACTIVE' THEN 1 ELSE NULL END
) STORED;

ALTER TABLE ticket_details
ADD CONSTRAINT uq_trip_seat_active UNIQUE (trip_id, seat_id, active_key);
```
- **Nguyên lý hoạt động**: Trong chuẩn SQL và MySQL, giá trị `NULL` trong chỉ mục `UNIQUE` không bị coi là trùng lặp. Khi vé ở trạng thái `ACTIVE`, `active_key = 1`, đảm bảo **không bao giờ có 2 vé ACTIVE cùng trip_id và seat_id**. Khi vé bị hủy (`CANCELLED`), `active_key` chuyển thành `NULL`, ghế lập tức được giải phóng để hành khách khác đặt mà vẫn giữ nguyên lịch sử vé cũ!

---

## 5. Tài khoản Thử nghiệm (Demo Accounts)

Tất cả các tài khoản mặc định sử dụng mật khẩu: `password`

| STT | Tên tài khoản | Email | Mật khẩu | Quyền hạn (Role) | Hạng thành viên | Đặc quyền |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| 1 | `admin` | `admin@busgo.vn` | `password` | `ROLE_ADMIN` | - | Toàn quyền quản trị hệ thống, báo cáo, quản lý chuyến & vé |
| 2 | `customer` | `customer@busgo.vn` | `password` | `ROLE_CUSTOMER` | `REGULAR` | Khách hàng mới (Giảm giá 0%) |
| 3 | `thanhtrung` | `thanhtrung@busgo.vn` | `thanhtrung` | `ROLE_CUSTOMER` | `MEMBER` | Khách hàng thân thiết (Tự động giảm 5% mọi chuyến) |
| 4 | `kieuanh` | `kieuanh@busgo.vn` | `kieuanh` | `ROLE_CUSTOMER` | `VIP` | Khách hàng VIP (Tự động giảm 10% mọi chuyến) |

---

## 6. Hướng dẫn Cài đặt & Khởi chạy (Step-by-Step)

### Bước 1: Yêu cầu môi trường
- **Java Development Kit (JDK)**: Phiên bản 17 (Eclipse Adoptium Temurin hoặc Oracle JDK 17).
- **Apache Maven**: Phiên bản 3.8.0 trở lên.
- **MySQL Server**: Phiên bản 8.0 trở lên (khuyên dùng MySQL Workbench hoặc DBeaver để quản lý).

### Bước 2: Khởi tạo Cơ sở Dữ liệu
1. Mở công cụ quản lý MySQL (MySQL Workbench / Navicat / Command Line).
2. Tạo database và nạp dữ liệu mẫu bằng cách chạy toàn bộ file SQL có sẵn trong dự án:
   - Đường dẫn file: `database/bus_ticket_booking.sql`
   ```sql
   -- Hoặc chạy lệnh trong MySQL CLI:
   source <đường_dẫn_dự_án>/database/bus_ticket_booking.sql;
   ```
   *File SQL này đã bao gồm đầy đủ cấu trúc 14 bảng, ràng buộc Trigger/Generated Column, và dữ liệu mẫu phong phú (5 nhà xe, 6 xe khách, 204 vị trí ghế, 10 chuyến xe, các mã khuyến mãi `HE2026`, `VIPBUS`).*

### Bước 3: Cấu hình Datasource trong Dự án
Mở file `src/main/resources/application.properties` và điều chỉnh thông tin kết nối MySQL của máy bạn (nếu có khác biệt):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bus_ticket_booking?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=root
```

### Bước 4: Mở dự án trong IntelliJ IDEA
1. Mở **IntelliJ IDEA** $\rightarrow$ Chọn **File** $\rightarrow$ **Open...** $\rightarrow$ Trỏ tới thư mục gốc dự án: `Test OOP tự làm`.
2. IntelliJ sẽ tự động nhận diện file `pom.xml`. Bấm **Load Maven Project** để IntelliJ tải đầy đủ thư viện.
3. Kiểm tra Project SDK: Vào **File** $\rightarrow$ **Project Structure** $\rightarrow$ **Project** $\rightarrow$ Chọn **SDK 17**.

### Bước 5: Chạy Ứng dụng
- **Cách 1 (Từ IDE)**: Mở file `src/main/java/com/example/booking/BookingApplication.java` $\rightarrow$ Nhấn nút **Run** (biểu tượng tam giác xanh).
- **Cách 2 (Từ dòng lệnh Terminal)**:
  ```bash
  mvn spring-boot:run
  ```

### Bước 6: Truy cập và Trải nghiệm
- **Trang chủ Khách hàng**: [http://localhost:8080/](http://localhost:8080/)
- **Đăng nhập**: [http://localhost:8080/login](http://localhost:8080/login)
- **Kênh Quản trị (Admin Dashboard)**: [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard)
- **Tra cứu vé trực tiếp (không cần login)**: [http://localhost:8080/lookup](http://localhost:8080/lookup)

---

## 7. Các Chức năng Chính & Kịch bản Kiểm thử

### 7.1. Phân hệ Khách hàng (Customer Web Portal)
1. **Tìm kiếm chuyến xe thông minh**:
   - Tìm kiếm tuyến TP. Hồ Chí Minh đi Nha Trang / Đà Lạt / Cần Thơ.
   - Bộ lọc đa chiều: Lọc theo khung giờ xuất phát (Sáng, Chiều, Tối), hãng xe, loại xe (Giường nằm, Limousine), khoảng giá.
2. **Sơ đồ chọn ghế 2 tầng trực quan**:
   - Hiển thị trực quan Tầng 1 (A01 - A18) và Tầng 2 (B01 - B18).
   - Màu sắc trạng thái: Xanh lá (Còn trống), Vàng (Đang giữ tạm), Đỏ (Đã bán).
   - Chọn tối đa 5 ghế/lần đặt; tự động kích hoạt **giữ ghế 10 phút** với đồng hồ đếm ngược.
3. **Thanh toán & Tính giá thông minh**:
   - Tự động nhận diện hạng thành viên (`MEMBER` -5%, `VIP` -10%).
   - Nhập mã khuyến mãi giảm giá (ví dụ: `HE2026` giảm 10%, `VIPBUS` giảm 50.000đ).
   - Xác nhận đặt vé thành công $\rightarrow$ Cấp mã vé `TKT-xxxx` kèm **Mã QR vé điện tử**.
4. **Quản lý Vé & Hủy vé**:
   - Xem danh sách vé đã đặt trong "Vé của tôi".
   - Tính toán tiền hoàn tự động theo chính sách: hủy trước 24 giờ nhận hoàn 100%.

### 7.2. Phân hệ Quản trị (Admin Portal)
1. **Bảng điều khiển (Dashboard)**:
   - Thống kê 4 thẻ chỉ số: Tổng doanh thu, Số vé bán, Chuyến đang chạy, Khách hàng mới.
   - 4 Biểu đồ phân tích: Doanh thu 7 ngày gần nhất, tỷ lệ trạng thái vé (ACTIVE, CANCELLED), Top tuyến xe đông khách nhất, Top nhà xe doanh thu cao.
2. **Quản lý Chuyến xe & Nhà xe (CRUD)**:
   - Thêm/sửa chuyến đi; kiểm tra trùng lịch xe; validate thời gian đến phải sau thời gian đi.
   - Thêm xe mới tự động sinh sơ đồ ghế tầng 1 & tầng 2 theo cấu hình chỗ.
3. **Quản lý Vé & Ghế**:
   - Xem trực quan sơ đồ trạng thái ghế của từng chuyến đi bất kỳ.
   - Xem chi tiết vé, hủy vé sự cố hoặc hoàn tiền thủ công.
4. **Xuất Báo cáo Excel**:
   - Xuất file `.xlsx` danh sách vé, doanh thu theo khoảng thời gian với đầy đủ định dạng số tiền VNĐ.

---

## 8. Hướng dẫn Chạy Kiểm thử Tự động (Automated Tests)

Dự án đã được tích hợp bộ test tự động toàn diện với **JUnit 5**, **Mockito** và **Spring Security Test**:

```bash
# Chạy toàn bộ các bộ Unit Test và Web Flow Test:
mvn test

# Hoặc chạy kiểm thử cụ thể từng phần:
mvn test -Dtest=PricingStrategyTest        # Kiểm thử tính giảm giá theo hạng & coupon
mvn test -Dtest=CancellationPolicyTest     # Kiểm thử chính sách hoàn tiền hủy vé
mvn test -Dtest=SeatBookingServiceTest     # Kiểm thử giữ ghế & concurrency chống trùng
mvn test -Dtest=UserServiceTest            # Kiểm thử đăng ký & mã hóa mật khẩu
```

Kết quả kiểm thử đảm bảo **100% Pass** với 0 failure và 0 error.
