CREATE TABLE IF NOT EXISTS accounts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL CHECK(role IN ('ADMIN','DOCTOR')),
  active BOOLEAN NOT NULL DEFAULT 1
);;
CREATE TABLE IF NOT EXISTS doctors (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  account_id INTEGER NOT NULL UNIQUE REFERENCES accounts(id),
  full_name TEXT NOT NULL,
  specialization TEXT NOT NULL,
  phone TEXT,
  email TEXT,
  description TEXT
);;
CREATE TABLE IF NOT EXISTS clinics (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  address TEXT NOT NULL,
  phone TEXT NOT NULL,
  email TEXT,
  opening_time TEXT NOT NULL,
  closing_time TEXT NOT NULL
);;
CREATE TABLE IF NOT EXISTS dental_services (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  description TEXT,
  duration_minutes INTEGER NOT NULL CHECK(duration_minutes > 0),
  price NUMERIC NOT NULL CHECK(price >= 0),
  active BOOLEAN NOT NULL DEFAULT 1
);;
CREATE TABLE IF NOT EXISTS appointments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT NOT NULL UNIQUE,
  patient_name TEXT NOT NULL,
  phone TEXT NOT NULL,
  email TEXT,
  service_id INTEGER NOT NULL REFERENCES dental_services(id),
  preferred_date TEXT NOT NULL,
  preferred_time TEXT NOT NULL,
  note TEXT,
  doctor_id INTEGER REFERENCES doctors(id),
  appointment_date TEXT,
  start_time TEXT,
  end_time TEXT,
  status TEXT NOT NULL CHECK(status IN ('PENDING','ASSIGNED','CONFIRMED','COMPLETED','CANCELLED')),
  created_at TEXT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CHECK(status IN ('PENDING','CANCELLED') OR (doctor_id IS NOT NULL AND appointment_date IS NOT NULL AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time))
);;
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_day ON appointments(doctor_id, appointment_date, status);;
CREATE TABLE IF NOT EXISTS login_sessions (
  id TEXT PRIMARY KEY,
  account_id INTEGER NOT NULL REFERENCES accounts(id),
  expires_at TEXT NOT NULL
);;
-- These triggers also protect against overlapping writes from another process.
CREATE TRIGGER IF NOT EXISTS prevent_overlap_insert BEFORE INSERT ON appointments
WHEN NEW.status IN ('ASSIGNED','CONFIRMED','COMPLETED')
BEGIN
  SELECT RAISE(ABORT, 'doctor_schedule_overlap') WHERE EXISTS (
    SELECT 1 FROM appointments a WHERE a.doctor_id = NEW.doctor_id
    AND a.appointment_date = NEW.appointment_date
    AND a.status IN ('ASSIGNED','CONFIRMED','COMPLETED')
    AND a.start_time < NEW.end_time AND a.end_time > NEW.start_time
  );
END;;
CREATE TRIGGER IF NOT EXISTS prevent_overlap_update BEFORE UPDATE ON appointments
WHEN NEW.status IN ('ASSIGNED','CONFIRMED','COMPLETED')
BEGIN
  SELECT RAISE(ABORT, 'doctor_schedule_overlap') WHERE EXISTS (
    SELECT 1 FROM appointments a WHERE a.id <> NEW.id AND a.doctor_id = NEW.doctor_id
    AND a.appointment_date = NEW.appointment_date
    AND a.status IN ('ASSIGNED','CONFIRMED','COMPLETED')
    AND a.start_time < NEW.end_time AND a.end_time > NEW.start_time
  );
END;;
