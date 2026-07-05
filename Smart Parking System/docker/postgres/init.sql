-- =============================================================================
-- Smart Parking System - Database Initialization Script
-- =============================================================================
-- This script runs automatically on the first container start via
-- docker-entrypoint-initdb.d/
--
-- Synchronized with Spring Boot JPA entities & schema.sql
-- =============================================================================

-- Enum Types
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'DRIVER');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE slot_status AS ENUM ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE booking_status AS ENUM ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'COMPLETED', 'CANCELLED', 'EXPIRED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- =====================================================
-- Users Table
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20) NOT NULL DEFAULT 'DRIVER',
    balance         DECIMAL(12, 2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- =====================================================
-- Vehicles Table
-- =====================================================
CREATE TABLE IF NOT EXISTS vehicles (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    license_plate   VARCHAR(20) NOT NULL,
    plate_encrypted VARCHAR(255),
    vehicle_type    VARCHAR(50) NOT NULL,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_vehicles_user_id ON vehicles(user_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_license_plate ON vehicles(license_plate);

-- =====================================================
-- Parking Slots Table
-- =====================================================
CREATE TABLE IF NOT EXISTS parking_slots (
    id              BIGSERIAL PRIMARY KEY,
    slot_code       VARCHAR(10) NOT NULL UNIQUE,
    zone            VARCHAR(10) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    sensor_id       VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_parking_slots_status ON parking_slots(status);
CREATE INDEX IF NOT EXISTS idx_parking_slots_zone ON parking_slots(zone);

-- =====================================================
-- Bookings Table
-- =====================================================
CREATE TABLE IF NOT EXISTS bookings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    vehicle_id      BIGINT NOT NULL REFERENCES vehicles(id),
    slot_id         BIGINT NOT NULL REFERENCES parking_slots(id),
    booking_code    VARCHAR(50) NOT NULL UNIQUE,
    qr_code_data    TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    booked_from     TIMESTAMP NOT NULL,
    booked_until    TIMESTAMP NOT NULL,
    checked_in_at   TIMESTAMP,
    checked_out_at  TIMESTAMP,
    total_amount    DECIMAL(12, 2) DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_slot_id ON bookings(slot_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_booking_code ON bookings(booking_code);
CREATE INDEX IF NOT EXISTS idx_bookings_booked_until ON bookings(booked_until);

-- =====================================================
-- Transactions Table
-- =====================================================
CREATE TABLE IF NOT EXISTS transactions (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT REFERENCES bookings(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    amount          DECIMAL(12, 2) NOT NULL,
    payment_method  VARCHAR(30) NOT NULL DEFAULT 'WALLET',
    payment_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_booking_id ON transactions(booking_id);

-- =====================================================
-- Device Registry Table
-- =====================================================
CREATE TABLE IF NOT EXISTS device_registry (
    id              BIGSERIAL PRIMARY KEY,
    device_uid      VARCHAR(100) NOT NULL UNIQUE,
    device_type     VARCHAR(30) NOT NULL,
    location        VARCHAR(255),
    certificate_cn  VARCHAR(255),
    is_online       BOOLEAN NOT NULL DEFAULT FALSE,
    last_heartbeat  TIMESTAMP,
    firmware_version VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_device_registry_device_uid ON device_registry(device_uid);
CREATE INDEX IF NOT EXISTS idx_device_registry_device_type ON device_registry(device_type);

-- =====================================================
-- Security Audit Log Table
-- =====================================================
CREATE TABLE IF NOT EXISTS security_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    action          VARCHAR(100) NOT NULL,
    resource        VARCHAR(255),
    ip_address      VARCHAR(50),
    details         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON security_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON security_audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON security_audit_log(created_at);

-- =====================================================
-- SEED DATA
-- =====================================================

-- Admin user (password: admin123)
-- BCrypt hash generated with strength 12
INSERT INTO users (email, password_hash, full_name, phone, role, balance, is_active)
VALUES ('admin@smartparking.com', '$2a$12$LJ3m4ys3uz0b6Tf3fNaZXeVq8IFMwZMGqNOAHnNJoGpZsv4UfWbWe', 'System Admin', '0901234567', 'ADMIN', 0, TRUE)
ON CONFLICT (email) DO NOTHING;

-- Driver user (password: driver123)
INSERT INTO users (email, password_hash, full_name, phone, role, balance, is_active)
VALUES ('driver@smartparking.com', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Test Driver', '0909876543', 'DRIVER', 100000, TRUE)
ON CONFLICT (email) DO NOTHING;

-- Parking Slots: Zone A (A01-A10)
INSERT INTO parking_slots (slot_code, zone, status, sensor_id) VALUES
('A01', 'A', 'AVAILABLE', 'SENSOR-A01'),
('A02', 'A', 'AVAILABLE', 'SENSOR-A02'),
('A03', 'A', 'AVAILABLE', 'SENSOR-A03'),
('A04', 'A', 'AVAILABLE', 'SENSOR-A04'),
('A05', 'A', 'AVAILABLE', 'SENSOR-A05'),
('A06', 'A', 'AVAILABLE', 'SENSOR-A06'),
('A07', 'A', 'AVAILABLE', 'SENSOR-A07'),
('A08', 'A', 'AVAILABLE', 'SENSOR-A08'),
('A09', 'A', 'AVAILABLE', 'SENSOR-A09'),
('A10', 'A', 'AVAILABLE', 'SENSOR-A10')
ON CONFLICT (slot_code) DO NOTHING;

-- Parking Slots: Zone B (B01-B10)
INSERT INTO parking_slots (slot_code, zone, status, sensor_id) VALUES
('B01', 'B', 'AVAILABLE', 'SENSOR-B01'),
('B02', 'B', 'AVAILABLE', 'SENSOR-B02'),
('B03', 'B', 'AVAILABLE', 'SENSOR-B03'),
('B04', 'B', 'AVAILABLE', 'SENSOR-B04'),
('B05', 'B', 'AVAILABLE', 'SENSOR-B05'),
('B06', 'B', 'AVAILABLE', 'SENSOR-B06'),
('B07', 'B', 'AVAILABLE', 'SENSOR-B07'),
('B08', 'B', 'AVAILABLE', 'SENSOR-B08'),
('B09', 'B', 'AVAILABLE', 'SENSOR-B09'),
('B10', 'B', 'AVAILABLE', 'SENSOR-B10')
ON CONFLICT (slot_code) DO NOTHING;

-- Device Registry
INSERT INTO device_registry (device_uid, device_type, location, is_online, firmware_version) VALUES
('GATE-ENTRY-01', 'GATE', 'Main Entrance', TRUE, '1.0.0'),
('GATE-EXIT-01', 'GATE', 'Main Exit', TRUE, '1.0.0')
ON CONFLICT (device_uid) DO NOTHING;
