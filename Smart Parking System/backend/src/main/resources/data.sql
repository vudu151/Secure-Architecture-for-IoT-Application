-- =====================================================
-- Smart Parking System - Seed Data
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
