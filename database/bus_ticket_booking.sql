-- =====================================================================
-- BUS TICKET BOOKING MANAGEMENT SYSTEM - MySQL 8
-- File: database/bus_ticket_booking.sql
-- Mat khau demo cho TAT CA tai khoan: password   (BCrypt strength 10)
-- =====================================================================
DROP DATABASE IF EXISTS bus_ticket_booking;
CREATE DATABASE bus_ticket_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bus_ticket_booking;

-- ---------------------------------------------------------------------
-- 1. roles / users / user_roles  (n-n)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    password        VARCHAR(100) NOT NULL,
    membership_type ENUM('REGULAR','MEMBER','VIP') NOT NULL DEFAULT 'REGULAR',
    total_spent     DECIMAL(14,0) NOT NULL DEFAULT 0,
    avatar          VARCHAR(255),
    enabled         TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email),
    CONSTRAINT uk_users_phone    UNIQUE (phone),
    CONSTRAINT ck_users_spent    CHECK (total_spent >= 0),
    INDEX idx_users_fullname (full_name),
    INDEX idx_users_membership (membership_type)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. bus_companies / buses / seats
-- ---------------------------------------------------------------------
CREATE TABLE bus_companies (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    hotline      VARCHAR(20)  NOT NULL,
    email        VARCHAR(120),
    address      VARCHAR(255) NOT NULL,
    description  TEXT,
    logo         VARCHAR(255),
    rating_avg   DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    rating_count INT NOT NULL DEFAULT 0,
    status       ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_company_name UNIQUE (name),
    CONSTRAINT ck_company_rating CHECK (rating_avg BETWEEN 0 AND 5)
) ENGINE=InnoDB;

CREATE TABLE buses (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id    BIGINT NOT NULL,
    license_plate VARCHAR(20) NOT NULL,
    bus_type      ENUM('SLEEPER','LIMOUSINE','SEATER') NOT NULL,
    total_seats   INT NOT NULL,
    floors        TINYINT NOT NULL DEFAULT 2,
    status        ENUM('ACTIVE','MAINTENANCE','RETIRED') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_bus_plate UNIQUE (license_plate),
    CONSTRAINT fk_bus_company FOREIGN KEY (company_id) REFERENCES bus_companies(id),
    CONSTRAINT ck_bus_seats CHECK (total_seats > 0),
    CONSTRAINT ck_bus_floors CHECK (floors IN (1,2)),
    INDEX idx_bus_company (company_id),
    INDEX idx_bus_type (bus_type)
) ENGINE=InnoDB;

-- Seat template cua tung xe (so do ghe). Trang thai theo chuyen nam o ticket_details / seat_holds.
CREATE TABLE seats (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    bus_id     BIGINT NOT NULL,
    seat_code  VARCHAR(10) NOT NULL,           -- A01.. (tang 1), B01.. (tang 2)
    floor_no   TINYINT NOT NULL,
    seat_type  ENUM('NORMAL','VIP') NOT NULL DEFAULT 'NORMAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_seat_bus_code UNIQUE (bus_id, seat_code),
    CONSTRAINT fk_seat_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE CASCADE,
    CONSTRAINT ck_seat_floor CHECK (floor_no IN (1,2))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. promotions / bus_trips
-- ---------------------------------------------------------------------
CREATE TABLE promotions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(30) NOT NULL,
    description    VARCHAR(255),
    discount_type  ENUM('PERCENT','AMOUNT') NOT NULL,
    discount_value DECIMAL(12,0) NOT NULL,
    max_discount   DECIMAL(12,0),
    min_order      DECIMAL(12,0) NOT NULL DEFAULT 0,
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL,
    usage_limit    INT NOT NULL DEFAULT 100,
    used_count     INT NOT NULL DEFAULT 0,
    active         TINYINT(1) NOT NULL DEFAULT 1,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_promo_code UNIQUE (code),
    CONSTRAINT ck_promo_value CHECK (discount_value > 0),
    CONSTRAINT ck_promo_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_promo_used CHECK (used_count >= 0 AND used_count <= usage_limit)
) ENGINE=InnoDB;

CREATE TABLE bus_trips (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_code      VARCHAR(20) NOT NULL,
    bus_id         BIGINT NOT NULL,
    origin         VARCHAR(100) NOT NULL,
    destination    VARCHAR(100) NOT NULL,
    pickup_point   VARCHAR(255),
    dropoff_point  VARCHAR(255),
    departure_time DATETIME NOT NULL,
    arrival_time   DATETIME NOT NULL,
    base_price     DECIMAL(12,0) NOT NULL,
    vip_surcharge  DECIMAL(12,0) NOT NULL DEFAULT 0,
    status         ENUM('SCHEDULED','RUNNING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_trip_code UNIQUE (trip_code),
    CONSTRAINT fk_trip_bus FOREIGN KEY (bus_id) REFERENCES buses(id),
    CONSTRAINT ck_trip_price CHECK (base_price > 0),
    CONSTRAINT ck_trip_vip CHECK (vip_surcharge >= 0),
    CONSTRAINT ck_trip_route CHECK (origin <> destination),
    CONSTRAINT ck_trip_time CHECK (arrival_time > departure_time),
    INDEX idx_trip_search (origin, destination, departure_time),
    INDEX idx_trip_departure (departure_time),
    INDEX idx_trip_status (status),
    INDEX idx_trip_bus (bus_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. tickets / ticket_details  (ticket 1-n details)
-- ---------------------------------------------------------------------
CREATE TABLE tickets (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_code        VARCHAR(20) NOT NULL,
    user_id            BIGINT NOT NULL,
    trip_id            BIGINT NOT NULL,
    promotion_id       BIGINT NULL,
    passenger_name     VARCHAR(100) NOT NULL,
    passenger_phone    VARCHAR(15)  NOT NULL,
    passenger_email    VARCHAR(120),
    subtotal           DECIMAL(12,0) NOT NULL,           -- tong gia ve goc
    vip_surcharge_total DECIMAL(12,0) NOT NULL DEFAULT 0,
    member_discount    DECIMAL(12,0) NOT NULL DEFAULT 0,
    promo_discount     DECIMAL(12,0) NOT NULL DEFAULT 0,
    total_amount       DECIMAL(12,0) NOT NULL,
    status             ENUM('PENDING','PAID','CANCELLED','EXPIRED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    cancel_reason      VARCHAR(255),
    cancelled_at       DATETIME NULL,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ticket_code UNIQUE (ticket_code),
    CONSTRAINT fk_ticket_user  FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ticket_trip  FOREIGN KEY (trip_id) REFERENCES bus_trips(id),
    CONSTRAINT fk_ticket_promo FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT ck_ticket_amount CHECK (subtotal > 0 AND total_amount >= 0
        AND member_discount >= 0 AND promo_discount >= 0 AND vip_surcharge_total >= 0),
    INDEX idx_ticket_user (user_id),
    INDEX idx_ticket_trip (trip_id),
    INDEX idx_ticket_status (status),
    INDEX idx_ticket_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE ticket_details (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT NOT NULL,
    trip_id    BIGINT NOT NULL,                  -- denormalize co chu dich de rang buoc unique
    seat_id    BIGINT NOT NULL,
    seat_code  VARCHAR(10) NOT NULL,
    seat_type  ENUM('NORMAL','VIP') NOT NULL,
    price      DECIMAL(12,0) NOT NULL,
    surcharge  DECIMAL(12,0) NOT NULL DEFAULT 0,
    status     ENUM('ACTIVE','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    -- = 1 khi ACTIVE, NULL khi CANCELLED => UNIQUE bo qua NULL: huy ve se nha ghe
    active_key TINYINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', 1, NULL)) STORED,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_td_trip_seat_active UNIQUE (trip_id, seat_id, active_key),  -- CHONG DAT TRUNG GHE
    CONSTRAINT fk_td_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_td_trip   FOREIGN KEY (trip_id)   REFERENCES bus_trips(id),
    CONSTRAINT fk_td_seat   FOREIGN KEY (seat_id)   REFERENCES seats(id),
    CONSTRAINT ck_td_price  CHECK (price > 0 AND surcharge >= 0),
    INDEX idx_td_ticket (ticket_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. seat_holds  (giu ghe tam 10 phut)
-- ---------------------------------------------------------------------
CREATE TABLE seat_holds (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id    BIGINT NOT NULL,
    seat_id    BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    held_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    status     ENUM('HELD','CONFIRMED','RELEASED','EXPIRED') NOT NULL DEFAULT 'HELD',
    -- chi 1 ban ghi HELD cho moi (chuyen, ghe)
    active_key TINYINT GENERATED ALWAYS AS (IF(status = 'HELD', 1, NULL)) STORED,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_hold_trip_seat_active UNIQUE (trip_id, seat_id, active_key),
    CONSTRAINT fk_hold_trip FOREIGN KEY (trip_id) REFERENCES bus_trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_hold_seat FOREIGN KEY (seat_id) REFERENCES seats(id),
    CONSTRAINT fk_hold_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_hold_time CHECK (expires_at > held_at),
    INDEX idx_hold_expiry (status, expires_at),
    INDEX idx_hold_user (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. payments / reviews / notifications
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id        BIGINT NOT NULL,
    transaction_code VARCHAR(30) NOT NULL,
    method           ENUM('CASH','BANK_TRANSFER','E_WALLET') NOT NULL,
    amount           DECIMAL(12,0) NOT NULL,
    status           ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    -- = 1 khi SUCCESS => moi ve chi co toi da 1 thanh toan thanh cong
    success_key      TINYINT GENERATED ALWAYS AS (IF(status = 'SUCCESS', 1, NULL)) STORED,
    paid_at          DATETIME NULL,
    note             VARCHAR(255),
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_txn UNIQUE (transaction_code),
    CONSTRAINT uk_payment_ticket_success UNIQUE (ticket_id, success_key),
    CONSTRAINT fk_payment_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    INDEX idx_payment_status (status),
    INDEX idx_payment_paid_at (paid_at)
) ENGINE=InnoDB;

CREATE TABLE reviews (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT NOT NULL,                  -- 1-1 voi ve
    user_id    BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    rating     TINYINT NOT NULL,
    comment    VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_ticket UNIQUE (ticket_id),
    CONSTRAINT fk_review_ticket  FOREIGN KEY (ticket_id)  REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_company FOREIGN KEY (company_id) REFERENCES bus_companies(id) ON DELETE CASCADE,
    CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 5),
    INDEX idx_review_company (company_id)
) ENGINE=InnoDB;

CREATE TABLE notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    title      VARCHAR(150) NOT NULL,
    content    VARCHAR(500) NOT NULL,
    type       ENUM('BOOKING','PAYMENT','CANCEL','PROMOTION','SYSTEM') NOT NULL DEFAULT 'SYSTEM',
    is_read    TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_read (user_id, is_read)
) ENGINE=InnoDB;

-- =====================================================================
-- DU LIEU MAU
-- =====================================================================
INSERT INTO roles (name, description) VALUES
 ('ROLE_ADMIN','Quan tri vien'), ('ROLE_CUSTOMER','Khach hang');

-- password = "password"
SET @pw = '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi';
INSERT INTO users (full_name, username, email, phone, password, membership_type, total_spent) VALUES
 ('Quản trị viên', 'admin',     'admin@busgo.vn',    '0900000001', @pw, 'VIP',     0),
 ('Nguyễn Văn An', 'customer',  'customer@busgo.vn', '0900000002', @pw, 'REGULAR', 0),
 ('Trần Thị Bình', 'binhtran',  'binh@busgo.vn',     '0900000003', @pw, 'MEMBER',  3500000),
 ('Lê Minh Cường', 'cuongle',   'cuong@busgo.vn',    '0900000004', @pw, 'VIP',     12000000);
INSERT INTO user_roles VALUES (1,1),(2,2),(3,2),(4,2);

INSERT INTO bus_companies (name, hotline, email, address, description, rating_avg, rating_count) VALUES
 ('Hoàng Long',   '1900 6067', 'cskh@hoanglong.vn', '265 Mai Động, Hoàng Mai, Hà Nội',        'Nhà xe uy tín tuyến Bắc - Trung.', 4.6, 320),
 ('Hải Vân',      '1900 6764', 'info@haivan.vn',    '12 Lê Hồng Phong, Hải Phòng',            'Chuyên tuyến Hà Nội - Hải Phòng.', 4.4, 210),
 ('Phương Trang', '1900 6746', 'hotro@futa.vn',     '80 Trần Hưng Đạo, Quận 1, TP.HCM',       'Mạng lưới toàn quốc.',             4.7, 540),
 ('Sao Việt',     '1900 6789', 'lienhe@saoviet.vn', '1 Lê Duẩn, Hải Châu, Đà Nẵng',           'Xe limousine cao cấp miền Trung.', 4.3, 150),
 ('Kumho Samco',  '1900 6855', 'cs@kumho.vn',       '440 Nguyễn Văn Linh, Quận 7, TP.HCM',    'Xe giường nằm đời mới.',           4.5, 260);

-- 6 xe: 40 cho giuong nam (2 tang x 20), 22 cho limousine (2 tang x 11)
INSERT INTO buses (company_id, license_plate, bus_type, total_seats, floors) VALUES
 (1,'29B-123.45','SLEEPER',  40,2),
 (2,'15B-678.90','SLEEPER',  40,2),
 (2,'15B-111.22','LIMOUSINE',22,2),
 (3,'51B-333.44','SLEEPER',  40,2),
 (4,'43B-555.66','LIMOUSINE',22,2),
 (5,'51B-777.88','SLEEPER',  40,2);

-- Sinh so do ghe: A01.. (tang 1), B01.. (tang 2); 4 ghe dau moi tang la VIP
INSERT INTO seats (bus_id, seat_code, floor_no, seat_type)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i+1 FROM n WHERE i < 20)
SELECT b.id,
       CONCAT(IF(f.fl = 1,'A','B'), LPAD(n.i,2,'0')),
       f.fl,
       IF(n.i <= 4,'VIP','NORMAL')
FROM buses b
JOIN (SELECT 1 AS fl UNION ALL SELECT 2) f ON f.fl <= b.floors
JOIN n ON n.i <= b.total_seats DIV b.floors
ORDER BY b.id, f.fl, n.i;

INSERT INTO promotions (code, description, discount_type, discount_value, max_discount, min_order, start_date, end_date, usage_limit) VALUES
 ('WELCOME10','Giảm 10% cho đơn đầu tiên','PERCENT',10,50000,100000, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 180 DAY), 500),
 ('SUMMER50K','Giảm 50.000đ mùa hè','AMOUNT',50000,NULL,300000, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 200);

-- Chuyen xe (ngay tuong doi so voi hom nay de luon co du lieu de test)
INSERT INTO bus_trips (trip_code, bus_id, origin, destination, pickup_point, dropoff_point, departure_time, arrival_time, base_price, vip_surcharge, status) VALUES
 ('HN-HP-001',1,'Hà Nội','Hải Phòng','Bến xe Gia Lâm','Bến xe Niệm Nghĩa', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY),'07:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:30:00'), 120000,30000,'SCHEDULED'),
 ('HN-HP-002',2,'Hà Nội','Hải Phòng','Bến xe Mỹ Đình','Bến xe Cầu Rào',     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY),'13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00'), 110000,30000,'SCHEDULED'),
 ('HN-HP-003',3,'Hà Nội','Hải Phòng','Bến xe Giáp Bát','Trung tâm Hải Phòng',TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY),'19:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY),'21:15:00'), 180000,50000,'SCHEDULED'),
 ('HP-HN-001',2,'Hải Phòng','Hà Nội','Bến xe Niệm Nghĩa','Bến xe Gia Lâm', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY),'17:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY),'19:30:00'), 120000,30000,'SCHEDULED'),
 ('SG-DL-001',4,'TP. Hồ Chí Minh','Đà Lạt','Bến xe Miền Đông','Bến xe Liên tỉnh Đà Lạt', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY),'22:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY),'05:00:00'), 300000,60000,'SCHEDULED'),
 ('DN-HUE-001',5,'Đà Nẵng','Huế','Bến xe Trung tâm Đà Nẵng','Bến xe phía Nam Huế', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY),'08:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY),'11:00:00'), 150000,40000,'SCHEDULED'),
 ('HN-DN-001',1,'Hà Nội','Đà Nẵng','Bến xe Nước Ngầm','Bến xe Trung tâm Đà Nẵng', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 5 DAY),'18:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 6 DAY),'08:00:00'), 450000,80000,'SCHEDULED'),
 ('SG-VT-001',6,'TP. Hồ Chí Minh','Vũng Tàu','Bến xe Miền Đông','Bến xe Vũng Tàu', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY),'06:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:30:00'), 140000,30000,'SCHEDULED'),
 -- chuyen da hoan thanh (phuc vu bao cao doanh thu)
 ('HN-HP-OLD1',1,'Hà Nội','Hải Phòng','Bến xe Gia Lâm','Bến xe Niệm Nghĩa', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY),'07:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY),'09:30:00'), 120000,30000,'COMPLETED'),
 ('HN-HP-OLD2',2,'Hà Nội','Hải Phòng','Bến xe Mỹ Đình','Bến xe Cầu Rào',     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'13:30:00'),  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),'16:00:00'),  110000,30000,'COMPLETED');

-- Ve mau. Ve 1: trip 1 (A01 VIP + A05) - khach 3 (MEMBER giam 5%) - da thanh toan
INSERT INTO tickets (ticket_code,user_id,trip_id,passenger_name,passenger_phone,passenger_email,subtotal,vip_surcharge_total,member_discount,promo_discount,total_amount,status) VALUES
 ('BG250001',3,1,'Trần Thị Bình','0900000003','binh@busgo.vn',240000,30000,13500,0,256500,'PAID'),
 ('BG250002',4,1,'Lê Minh Cường','0900000004','cuong@busgo.vn',120000,30000,15000,0,135000,'PAID'),   -- VIP giam 10%
 ('BG250003',2,2,'Nguyễn Văn An','0900000002','customer@busgo.vn',110000,0,0,0,110000,'PENDING'),
 ('BG250004',3,9,'Trần Thị Bình','0900000003','binh@busgo.vn',240000,30000,13500,0,256500,'COMPLETED'),
 ('BG250005',2,10,'Nguyễn Văn An','0900000002','customer@busgo.vn',220000,0,0,0,220000,'COMPLETED'),
 ('BG250006',2,3,'Nguyễn Văn An','0900000002','customer@busgo.vn',180000,0,0,0,180000,'CANCELLED');
UPDATE tickets SET cancel_reason='Khách đổi lịch', cancelled_at=NOW() WHERE ticket_code='BG250006';

INSERT INTO ticket_details (ticket_id, trip_id, seat_id, seat_code, seat_type, price, surcharge, status)
SELECT t.id, t.trip_id, s.id, s.seat_code, s.seat_type, tr.base_price,
       IF(s.seat_type='VIP', tr.vip_surcharge, 0),
       IF(t.status='CANCELLED','CANCELLED','ACTIVE')
FROM (
  SELECT 'BG250001' c,'A01' sc UNION ALL SELECT 'BG250001','A05'
  UNION ALL SELECT 'BG250002','A02'
  UNION ALL SELECT 'BG250003','A06'
  UNION ALL SELECT 'BG250004','A03' UNION ALL SELECT 'BG250004','A07'
  UNION ALL SELECT 'BG250005','A08' UNION ALL SELECT 'BG250005','A09'
  UNION ALL SELECT 'BG250006','A10'
) x
JOIN tickets t   ON t.ticket_code = x.c
JOIN bus_trips tr ON tr.id = t.trip_id
JOIN seats s     ON s.bus_id = tr.bus_id AND s.seat_code = x.sc;

-- Giu ghe tam dang con hieu luc cho ve PENDING (10 phut) - se het han khi scheduler chay
INSERT INTO seat_holds (trip_id, seat_id, user_id, held_at, expires_at, status)
SELECT 2, s.id, 2, NOW(), DATE_ADD(NOW(), INTERVAL 10 MINUTE), 'HELD'
FROM seats s JOIN bus_trips tr ON tr.id = 2 AND s.bus_id = tr.bus_id WHERE s.seat_code = 'A06';

INSERT INTO payments (ticket_id, transaction_code, method, amount, status, paid_at, note) VALUES
 (1,'TXN-DEMO-0001','E_WALLET',256500,'SUCCESS',NOW(),'Demo - gia lap'),
 (2,'TXN-DEMO-0002','BANK_TRANSFER',135000,'SUCCESS',NOW(),'Demo - gia lap'),
 (4,'TXN-DEMO-0004','BANK_TRANSFER',256500,'SUCCESS',DATE_SUB(NOW(), INTERVAL 20 DAY),'Demo - gia lap'),
 (5,'TXN-DEMO-0005','CASH',220000,'SUCCESS',DATE_SUB(NOW(), INTERVAL 5 DAY),'Demo - gia lap'),
 (6,'TXN-DEMO-0006','E_WALLET',180000,'REFUNDED',DATE_SUB(NOW(), INTERVAL 1 DAY),'Demo - gia lap');

INSERT INTO reviews (ticket_id,user_id,company_id,rating,comment) VALUES
 (4,3,1,5,'Xe sạch sẽ, đúng giờ, tài xế thân thiện.'),
 (5,2,2,4,'Chuyến đi ổn, ghế thoải mái.');

INSERT INTO notifications (user_id,title,content,type) VALUES
 (3,'Đặt vé thành công','Vé BG250001 đã được thanh toán.','PAYMENT'),
 (2,'Vé chờ thanh toán','Vé BG250003 sẽ hết hạn giữ chỗ sau 10 phút.','BOOKING'),
 (2,'Khuyến mãi','Dùng mã WELCOME10 giảm 10% cho chuyến tiếp theo.','PROMOTION');

-- =====================================================================
-- TRUY VAN KIEM TRA NHANH
-- =====================================================================
-- SELECT COUNT(*) FROM seats;                         -- 4 xe x 40 + 2 xe x 22 = 204
-- Thu dat trung ghe (se loi Duplicate entry):
-- INSERT INTO ticket_details (ticket_id,trip_id,seat_id,seat_code,seat_type,price)
--   SELECT 3,1,seat_id,seat_code,seat_type,price FROM ticket_details WHERE ticket_id=1 LIMIT 1;
