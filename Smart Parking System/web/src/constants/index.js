// API & WebSocket URLs
export const API_BASE_URL = 'http://localhost:8080/api/v1';
export const WS_URL = 'http://localhost:8080/ws';

// Parking Slot Statuses
export const SLOT_STATUS = {
  AVAILABLE: {
    key: 'AVAILABLE',
    label: 'Trống',
    color: '#52c41a',
    bgColor: 'rgba(82, 196, 26, 0.15)',
    tag: 'success',
  },
  OCCUPIED: {
    key: 'OCCUPIED',
    label: 'Đang đỗ',
    color: '#ff4d4f',
    bgColor: 'rgba(255, 77, 79, 0.15)',
    tag: 'error',
  },
  RESERVED: {
    key: 'RESERVED',
    label: 'Đã đặt',
    color: '#faad14',
    bgColor: 'rgba(250, 173, 20, 0.15)',
    tag: 'warning',
  },
  MAINTENANCE: {
    key: 'MAINTENANCE',
    label: 'Bảo trì',
    color: '#8c8c8c',
    bgColor: 'rgba(140, 140, 140, 0.15)',
    tag: 'default',
  },
};

// Booking Statuses
export const BOOKING_STATUS = {
  PENDING: {
    key: 'PENDING',
    label: 'Chờ xử lý',
    color: '#faad14',
    tag: 'warning',
  },
  CONFIRMED: {
    key: 'CONFIRMED',
    label: 'Đã xác nhận',
    color: '#1677ff',
    tag: 'processing',
  },
  CHECKED_IN: {
    key: 'CHECKED_IN',
    label: 'Đã vào',
    color: '#52c41a',
    tag: 'success',
  },
  CHECKED_OUT: {
    key: 'CHECKED_OUT',
    label: 'Đã ra',
    color: '#8c8c8c',
    tag: 'default',
  },
  CANCELLED: {
    key: 'CANCELLED',
    label: 'Đã hủy',
    color: '#ff4d4f',
    tag: 'error',
  },
};

// Device Types
export const DEVICE_TYPE = {
  SENSOR: { label: 'Cảm biến', color: 'blue' },
  CAMERA: { label: 'Camera', color: 'purple' },
  BARRIER: { label: 'Barrier', color: 'orange' },
  DISPLAY: { label: 'Bảng hiển thị', color: 'cyan' },
};

// Audit Log Actions
export const AUDIT_ACTIONS = {
  LOGIN: { label: 'Đăng nhập', color: 'blue' },
  LOGOUT: { label: 'Đăng xuất', color: 'default' },
  BOOKING_CREATE: { label: 'Tạo booking', color: 'green' },
  BOOKING_CANCEL: { label: 'Hủy booking', color: 'red' },
  CHECK_IN: { label: 'Check-in', color: 'cyan' },
  CHECK_OUT: { label: 'Check-out', color: 'purple' },
  GATE_OPEN: { label: 'Mở barrier', color: 'orange' },
  GATE_CLOSE: { label: 'Đóng barrier', color: 'volcano' },
  USER_UPDATE: { label: 'Cập nhật user', color: 'gold' },
  DEVICE_UPDATE: { label: 'Cập nhật thiết bị', color: 'lime' },
};

// Format VND currency
export const formatVND = (amount) => {
  if (amount === null || amount === undefined) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
};

// Format datetime
export const formatDateTime = (dateStr) => {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return d.toLocaleString('vi-VN');
};
