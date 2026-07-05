import axiosInstance from './axiosInstance';

export const adminApi = {
  // Dashboard
  getDashboard: () =>
    axiosInstance.get('/admin/dashboard'),

  // Revenue
  getRevenue: (params) =>
    axiosInstance.get('/admin/revenue', { params }),

  // Users
  getUsers: (params) =>
    axiosInstance.get('/admin/users', { params }),

  getUserById: (id) =>
    axiosInstance.get(`/admin/users/${id}`),

  toggleUserActive: (id) =>
    axiosInstance.put(`/admin/users/${id}/toggle-active`),

  // Devices
  getDevices: (params) =>
    axiosInstance.get('/admin/devices', { params }),

  getDeviceById: (id) =>
    axiosInstance.get(`/admin/devices/${id}`),

  // Gate Control
  controlGate: (gateId, action) =>
    axiosInstance.post(`/admin/gate/${gateId}/control`, { action }),

  getGateStatus: () =>
    axiosInstance.get('/admin/gates/status'),

  // Audit Logs
  getAuditLogs: (params) =>
    axiosInstance.get('/admin/audit-logs', { params }),
};

export default adminApi;
