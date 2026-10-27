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
    -- Serialize overlap checks for the same doctor across concurrent transactions.
    PERFORM pg_advisory_xact_lock(NEW.doctor_id);
    IF EXISTS (
      SELECT 1 FROM appointments a
      WHERE (TG_OP = 'INSERT' OR a.id <> NEW.id)
        AND a.doctor_id = NEW.doctor_id
        AND a.appointment_date = NEW.appointment_date
        AND a.status IN ('ASSIGNED', 'CONFIRMED', 'COMPLETED')
        AND a.start_time < NEW.end_time AND a.end_time > NEW.start_time
    ) THEN
      RAISE EXCEPTION 'doctor_schedule_overlap' USING ERRCODE = '23P01';
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
