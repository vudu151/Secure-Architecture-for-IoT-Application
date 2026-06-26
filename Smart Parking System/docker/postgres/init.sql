-- =============================================================================
-- Smart Parking System - PostgreSQL Initialization Script
-- =============================================================================
-- This script runs automatically on first container start via
-- docker-entrypoint-initdb.d/
--
-- Creates:
--   1. Extensions (pgcrypto, uuid-ossp)
--   2. Custom types (enums)
--   3. Tables (users, vehicles, parking_slots, devices, sessions, etc.)
--   4. Indexes for query performance
--   5. Seed data (admin user, sample slots, devices)
-- =============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- 1. Custom Types (Enums)
-- =============================================================================

CREATE TYPE user_role AS ENUM ('ADMIN', 'DRIVER');
CREATE TYPE slot_status AS ENUM ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE');
CREATE TYPE slot_zone AS ENUM ('A', 'B');
CREATE TYPE device_type AS ENUM ('ESP32_SENSOR', 'RPI_GATE');
CREATE TYPE device_status AS ENUM ('ONLINE', 'OFFLINE', 'MAINTENANCE');
CREATE TYPE session_status AS ENUM ('ACTIVE', 'COMPLETED', 'CANCELLED');
CREATE TYPE gate_type AS ENUM ('ENTRY', 'EXIT');
CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');
CREATE TYPE payment_method AS ENUM ('CREDIT_CARD', 'E_WALLET', 'CASH', 'QR_CODE');

-- =============================================================================
-- 2. Users Table
-- =============================================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username        VARCHAR(50) UNIQUE NOT NULL,
    email           VARCHAR(100) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            user_role NOT NULL DEFAULT 'DRIVER',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,

    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);

-- =============================================================================
-- 3. Vehicles Table
-- =============================================================================

CREATE TABLE vehicles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plate_number    VARCHAR(20) UNIQUE NOT NULL,
    vehicle_type    VARCHAR(30) NOT NULL DEFAULT 'CAR',
    brand           VARCHAR(50),
    color           VARCHAR(30),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicles_user_id ON vehicles(user_id);
CREATE INDEX idx_vehicles_plate_number ON vehicles(plate_number);

-- =============================================================================
-- 4. Parking Slots Table
-- =============================================================================

CREATE TABLE parking_slots (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slot_code       VARCHAR(10) UNIQUE NOT NULL,    -- e.g., 'A01', 'B05'
    zone            slot_zone NOT NULL,
    slot_number     INTEGER NOT NULL,
    status          slot_status NOT NULL DEFAULT 'AVAILABLE',
    floor_level     INTEGER NOT NULL DEFAULT 1,
    is_handicapped  BOOLEAN NOT NULL DEFAULT FALSE,
    is_ev_charging  BOOLEAN NOT NULL DEFAULT FALSE,
    hourly_rate     DECIMAL(10, 2) NOT NULL DEFAULT 10000.00,  -- VND
    last_status_change TIMESTAMPTZ DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_zone_slot UNIQUE (zone, slot_number),
    CONSTRAINT chk_slot_number CHECK (slot_number > 0 AND slot_number <= 100)
);

CREATE INDEX idx_parking_slots_status ON parking_slots(status);
CREATE INDEX idx_parking_slots_zone ON parking_slots(zone);
CREATE INDEX idx_parking_slots_code ON parking_slots(slot_code);

-- =============================================================================
-- 5. IoT Devices Table
-- =============================================================================

CREATE TABLE devices (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id       VARCHAR(50) UNIQUE NOT NULL,    -- e.g., 'esp32_slot_a01'
    device_type     device_type NOT NULL,
    status          device_status NOT NULL DEFAULT 'OFFLINE',
    slot_id         UUID REFERENCES parking_slots(id) ON DELETE SET NULL,
    gate_id         VARCHAR(20),                     -- e.g., 'gate1', 'gate2'
    firmware_version VARCHAR(20),
    ip_address      INET,
    last_heartbeat  TIMESTAMPTZ,
    certificate_cn  VARCHAR(100),                    -- mTLS certificate CN
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_type ON devices(device_type);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_devices_slot_id ON devices(slot_id);

-- =============================================================================
-- 6. Parking Sessions Table
-- =============================================================================

CREATE TABLE parking_sessions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    vehicle_id      UUID REFERENCES vehicles(id) ON DELETE SET NULL,
    slot_id         UUID NOT NULL REFERENCES parking_slots(id),
    plate_number    VARCHAR(20) NOT NULL,
    entry_time      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    exit_time       TIMESTAMPTZ,
    duration_minutes INTEGER,
    status          session_status NOT NULL DEFAULT 'ACTIVE',
    entry_gate      VARCHAR(20),
    exit_gate       VARCHAR(20),
    entry_image_url VARCHAR(500),
    exit_image_url  VARCHAR(500),
    qr_code_token   VARCHAR(255) UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user_id ON parking_sessions(user_id);
CREATE INDEX idx_sessions_vehicle_id ON parking_sessions(vehicle_id);
CREATE INDEX idx_sessions_slot_id ON parking_sessions(slot_id);
CREATE INDEX idx_sessions_status ON parking_sessions(status);
CREATE INDEX idx_sessions_plate ON parking_sessions(plate_number);
CREATE INDEX idx_sessions_entry_time ON parking_sessions(entry_time);
CREATE INDEX idx_sessions_qr_token ON parking_sessions(qr_code_token);

-- =============================================================================
-- 7. Payments Table
-- =============================================================================

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id      UUID NOT NULL REFERENCES parking_sessions(id) ON DELETE CASCADE,
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'VND',
    payment_method  payment_method,
    payment_status  payment_status NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(100) UNIQUE,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_amount_positive CHECK (amount >= 0)
);

CREATE INDEX idx_payments_session_id ON payments(session_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(payment_status);

-- =============================================================================
-- 8. Gate Events Table (audit log for gate operations)
-- =============================================================================

CREATE TABLE gate_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    gate_id         VARCHAR(20) NOT NULL,
    gate_type       gate_type NOT NULL,
    event_type      VARCHAR(30) NOT NULL,         -- 'PLATE_DETECTED', 'QR_SCANNED', 'GATE_OPENED', 'GATE_CLOSED'
    plate_number    VARCHAR(20),
    session_id      UUID REFERENCES parking_sessions(id),
    device_id       VARCHAR(50),
    confidence      DECIMAL(5, 4),                -- ANPR confidence score
    image_url       VARCHAR(500),
    metadata        JSONB,                         -- Additional event data
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gate_events_gate_id ON gate_events(gate_id);
CREATE INDEX idx_gate_events_type ON gate_events(event_type);
CREATE INDEX idx_gate_events_plate ON gate_events(plate_number);
CREATE INDEX idx_gate_events_session ON gate_events(session_id);
CREATE INDEX idx_gate_events_created ON gate_events(created_at);

-- =============================================================================
-- 9. Reservations Table
-- =============================================================================

CREATE TABLE reservations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    slot_id         UUID NOT NULL REFERENCES parking_slots(id),
    vehicle_id      UUID REFERENCES vehicles(id),
    start_time      TIMESTAMPTZ NOT NULL,
    end_time        TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, CONFIRMED, CANCELLED, EXPIRED
    qr_code_data    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_reservation_time CHECK (end_time > start_time)
);

CREATE INDEX idx_reservations_user_id ON reservations(user_id);
CREATE INDEX idx_reservations_slot_id ON reservations(slot_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_time ON reservations(start_time, end_time);

-- =============================================================================
-- 10. Nonce Table (for replay attack prevention)
-- =============================================================================

CREATE TABLE used_nonces (
    id              BIGSERIAL PRIMARY KEY,
    nonce           VARCHAR(64) UNIQUE NOT NULL,
    used_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_nonces_nonce ON used_nonces(nonce);
CREATE INDEX idx_nonces_expires ON used_nonces(expires_at);

-- =============================================================================
-- 11. Refresh Tokens Table
-- =============================================================================

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(500) UNIQUE NOT NULL,
    device_info     VARCHAR(255),
    ip_address      INET,
    expires_at      TIMESTAMPTZ NOT NULL,
    is_revoked      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);

-- =============================================================================
-- 12. Auto-update trigger for updated_at
-- =============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_vehicles_updated_at
    BEFORE UPDATE ON vehicles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_parking_slots_updated_at
    BEFORE UPDATE ON parking_slots
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_devices_updated_at
    BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_sessions_updated_at
    BEFORE UPDATE ON parking_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_reservations_updated_at
    BEFORE UPDATE ON reservations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- 13. Seed Data
-- =============================================================================

-- --- Admin User ---
-- Password: admin123 (BCrypt hash)
INSERT INTO users (id, username, email, password_hash, full_name, phone, role, is_active, email_verified)
VALUES (
    uuid_generate_v4(),
    'admin',
    'admin@smartparking.vn',
    '$2a$12$LJ3m4ys3LkxSIkJEHKbPOOJLvJXMGH/tO2.YPPLFqp1HrGzMx3IZi',
    'System Administrator',
    '+84901234567',
    'ADMIN',
    TRUE,
    TRUE
);

-- --- Sample Driver User ---
-- Password: driver123 (BCrypt hash)
INSERT INTO users (id, username, email, password_hash, full_name, phone, role, is_active, email_verified)
VALUES (
    uuid_generate_v4(),
    'driver01',
    'driver01@example.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Nguyen Van A',
    '+84909876543',
    'DRIVER',
    TRUE,
    TRUE
);

-- --- Parking Slots: Zone A (A01-A10) ---
INSERT INTO parking_slots (slot_code, zone, slot_number, status, floor_level, hourly_rate)
VALUES
    ('A01', 'A', 1,  'AVAILABLE', 1, 10000.00),
    ('A02', 'A', 2,  'AVAILABLE', 1, 10000.00),
    ('A03', 'A', 3,  'AVAILABLE', 1, 10000.00),
    ('A04', 'A', 4,  'AVAILABLE', 1, 10000.00),
    ('A05', 'A', 5,  'AVAILABLE', 1, 10000.00),
    ('A06', 'A', 6,  'AVAILABLE', 1, 10000.00),
    ('A07', 'A', 7,  'AVAILABLE', 1, 10000.00),
    ('A08', 'A', 8,  'AVAILABLE', 1, 10000.00),
    ('A09', 'A', 9,  'AVAILABLE', 1, 10000.00),
    ('A10', 'A', 10, 'AVAILABLE', 1, 10000.00);

-- --- Parking Slots: Zone B (B01-B10) ---
INSERT INTO parking_slots (slot_code, zone, slot_number, status, floor_level, hourly_rate)
VALUES
    ('B01', 'B', 1,  'AVAILABLE', 1, 12000.00),
    ('B02', 'B', 2,  'AVAILABLE', 1, 12000.00),
    ('B03', 'B', 3,  'AVAILABLE', 1, 12000.00),
    ('B04', 'B', 4,  'AVAILABLE', 1, 12000.00),
    ('B05', 'B', 5,  'AVAILABLE', 1, 12000.00),
    ('B06', 'B', 6,  'AVAILABLE', 1, 12000.00),
    ('B07', 'B', 7,  'AVAILABLE', 1, 12000.00),
    ('B08', 'B', 8,  'AVAILABLE', 1, 12000.00),
    ('B09', 'B', 9,  'AVAILABLE', 1, 12000.00),
    ('B10', 'B', 10, 'AVAILABLE', 1, 12000.00);

-- --- IoT Devices: ESP32 Sensors (linked to slots) ---
-- We insert devices and link them to their corresponding slots
DO $$
DECLARE
    v_slot_id UUID;
    v_zone TEXT;
    v_num INTEGER;
BEGIN
    -- Zone A sensors
    FOR v_num IN 1..10 LOOP
        SELECT id INTO v_slot_id FROM parking_slots
        WHERE zone = 'A' AND slot_number = v_num;

        INSERT INTO devices (device_id, device_type, status, slot_id, firmware_version, certificate_cn)
        VALUES (
            'esp32_slot_a' || LPAD(v_num::TEXT, 2, '0'),
            'ESP32_SENSOR',
            'OFFLINE',
            v_slot_id,
            '1.0.0',
            'esp32_slot_a' || LPAD(v_num::TEXT, 2, '0')
        );
    END LOOP;

    -- Zone B sensors
    FOR v_num IN 1..10 LOOP
        SELECT id INTO v_slot_id FROM parking_slots
        WHERE zone = 'B' AND slot_number = v_num;

        INSERT INTO devices (device_id, device_type, status, slot_id, firmware_version, certificate_cn)
        VALUES (
            'esp32_slot_b' || LPAD(v_num::TEXT, 2, '0'),
            'ESP32_SENSOR',
            'OFFLINE',
            v_slot_id,
            '1.0.0',
            'esp32_slot_b' || LPAD(v_num::TEXT, 2, '0')
        );
    END LOOP;
END $$;

-- --- IoT Devices: Raspberry Pi Gate Controllers ---
INSERT INTO devices (device_id, device_type, status, gate_id, firmware_version, certificate_cn)
VALUES
    ('rpi_gate1', 'RPI_GATE', 'OFFLINE', 'gate1', '1.0.0', 'rpi_gate1'),
    ('rpi_gate2', 'RPI_GATE', 'OFFLINE', 'gate2', '1.0.0', 'rpi_gate2');

-- --- Sample Vehicle for driver01 ---
INSERT INTO vehicles (user_id, plate_number, vehicle_type, brand, color)
SELECT id, '29A-12345', 'CAR', 'Toyota', 'White'
FROM users WHERE username = 'driver01';

-- =============================================================================
-- 14. Cleanup job function (for expired nonces)
-- =============================================================================

CREATE OR REPLACE FUNCTION cleanup_expired_nonces()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM used_nonces WHERE expires_at < NOW();
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- Verification: Print summary
-- =============================================================================

DO $$
DECLARE
    user_count INTEGER;
    slot_count INTEGER;
    device_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count FROM users;
    SELECT COUNT(*) INTO slot_count FROM parking_slots;
    SELECT COUNT(*) INTO device_count FROM devices;

    RAISE NOTICE '============================================';
    RAISE NOTICE 'Smart Parking System - Database Initialized';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Users:         %', user_count;
    RAISE NOTICE 'Parking Slots: %', slot_count;
    RAISE NOTICE 'IoT Devices:   %', device_count;
    RAISE NOTICE '============================================';
END $$;
