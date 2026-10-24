CREATE TABLE IF NOT EXISTS accounts (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL CHECK(role IN ('ADMIN','DOCTOR')),
  active BOOLEAN NOT NULL DEFAULT TRUE
);;

CREATE TABLE IF NOT EXISTS doctors (
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT NOT NULL UNIQUE REFERENCES accounts(id),
  full_name VARCHAR(255) NOT NULL,
  specialization VARCHAR(255) NOT NULL,
  phone VARCHAR(50),
  email VARCHAR(255),
  description TEXT
);;

CREATE TABLE IF NOT EXISTS clinics (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  address VARCHAR(500) NOT NULL,
  phone VARCHAR(50) NOT NULL,
  email VARCHAR(255),
  opening_time VARCHAR(20) NOT NULL,
  closing_time VARCHAR(20) NOT NULL
);;

CREATE TABLE IF NOT EXISTS dental_services (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  duration_minutes INTEGER NOT NULL CHECK(duration_minutes > 0),
  price NUMERIC NOT NULL CHECK(price >= 0),
  active BOOLEAN NOT NULL DEFAULT TRUE
);;

CREATE TABLE IF NOT EXISTS appointments (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(100) NOT NULL UNIQUE,
  patient_name VARCHAR(255) NOT NULL,
  phone VARCHAR(50) NOT NULL,
  email VARCHAR(255),
  service_id BIGINT NOT NULL REFERENCES dental_services(id),
  preferred_date VARCHAR(20) NOT NULL,
  preferred_time VARCHAR(20) NOT NULL,
  note TEXT,
  doctor_id BIGINT REFERENCES doctors(id),
  appointment_date VARCHAR(20),
  start_time VARCHAR(20),
  end_time VARCHAR(20),
  status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING','ASSIGNED','CONFIRMED','COMPLETED','CANCELLED')),
  created_at VARCHAR(50) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CHECK(status IN ('PENDING','CANCELLED') OR (doctor_id IS NOT NULL AND appointment_date IS NOT NULL AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time))
);;

CREATE INDEX IF NOT EXISTS idx_appointments_doctor_day ON appointments(doctor_id, appointment_date, status);;

CREATE TABLE IF NOT EXISTS login_sessions (
  id VARCHAR(255) PRIMARY KEY,
  account_id BIGINT NOT NULL REFERENCES accounts(id),
  expires_at VARCHAR(50) NOT NULL
);;

CREATE OR REPLACE FUNCTION check_appointment_overlap()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status IN ('ASSIGNED', 'CONFIRMED', 'COMPLETED') THEN
    IF EXISTS (
      SELECT 1 FROM appointments a
      WHERE (TG_OP = 'INSERT' OR a.id <> NEW.id)
        AND a.doctor_id = NEW.doctor_id
        AND a.appointment_date = NEW.appointment_date
        AND a.status IN ('ASSIGNED', 'CONFIRMED', 'COMPLETED')
        AND a.start_time < NEW.end_time AND a.end_time > NEW.start_time
    ) THEN
      RAISE EXCEPTION 'doctor_schedule_overlap';
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_prevent_overlap_insert ON appointments;;
CREATE TRIGGER trg_prevent_overlap_insert
BEFORE INSERT ON appointments
FOR EACH ROW EXECUTE FUNCTION check_appointment_overlap();;

DROP TRIGGER IF EXISTS trg_prevent_overlap_update ON appointments;;
CREATE TRIGGER trg_prevent_overlap_update
BEFORE UPDATE ON appointments
FOR EACH ROW EXECUTE FUNCTION check_appointment_overlap();;

-- ==============================================================================
-- DỮ LIỆU DEMO THỐNG NHẤT VỚI BÊN SQLITE
-- ==============================================================================

-- 1. Thong tin phong kham demo
INSERT INTO clinics (id, name, address, phone, email, opening_time, closing_time)
VALUES (1, 'Phong kham nha khoa Demo', 'Dia chi mau - cap nhat theo phong kham', '0900000000', 'clinic@example.com', '08:00', '17:00')
ON CONFLICT (id) DO NOTHING;;

-- 2. Danh muc 3 dich vu nha khoa chuan
INSERT INTO dental_services (id, name, description, duration_minutes, price, active)
VALUES
  (1, 'Kham tong quat', 'Dich vu mau', 30, 100000, TRUE),
  (2, 'Lay cao rang', 'Dich vu mau', 60, 300000, TRUE),
  (3, 'Nho rang', 'Dich vu mau', 60, 500000, TRUE)
ON CONFLICT (id) DO NOTHING;;

-- 3. Tai khoan dang nhap demo (admin: Admin123!, doctor1/doctor2: Doctor123!)
INSERT INTO accounts (id, username, password_hash, role, active)
VALUES
  (1, 'admin', '$2a$10$X.HLFZER9g/zeijB4WMjz.D2sZ39lnKTj5mIwA2b7mtylhZUahpVS', 'ADMIN', TRUE),
  (2, 'doctor1', '$2a$10$uRKgc41QylS1P3ubamcf1OIh9/v1Q/r0vZdBxjF2/A90OFjhyPJ9K', 'DOCTOR', TRUE),
  (3, 'doctor2', '$2a$10$uRKgc41QylS1P3ubamcf1OIh9/v1Q/r0vZdBxjF2/A90OFjhyPJ9K', 'DOCTOR', TRUE)
ON CONFLICT (username) DO NOTHING;;

-- 4. Ho so bac si demo tuong ung voi accounts
INSERT INTO doctors (id, account_id, full_name, specialization, phone, email, description)
VALUES
  (1, 2, 'Bac si Nguyen An', 'Nha khoa tong quat', '0900000001', 'doctor1@example.com', 'Du lieu mau'),
  (2, 3, 'Bac si Tran Binh', 'Phau thuat rang', '0900000002', 'doctor2@example.com', 'Du lieu mau')
ON CONFLICT (account_id) DO NOTHING;;

-- 5. Dong bo Sequence ID sau khi seed de tranh collision khi them moi
SELECT setval('accounts_id_seq', (SELECT COALESCE(MAX(id), 1) FROM accounts));;
SELECT setval('doctors_id_seq', (SELECT COALESCE(MAX(id), 1) FROM doctors));;
SELECT setval('clinics_id_seq', (SELECT COALESCE(MAX(id), 1) FROM clinics));;
SELECT setval('dental_services_id_seq', (SELECT COALESCE(MAX(id), 1) FROM dental_services));;
SELECT setval('appointments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM appointments));;
