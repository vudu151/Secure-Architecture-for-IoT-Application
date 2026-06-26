import axiosInstance from './axiosInstance';

export const slotApi = {
  getAllSlots: (params) =>
    axiosInstance.get('/slots', { params }),

  getAvailableSlots: () =>
    axiosInstance.get('/slots/available'),

  getSlotById: (id) =>
    axiosInstance.get(`/slots/${id}`),

  updateSlotStatus: (id, status) =>
    axiosInstance.patch(`/slots/${id}/status`, { status }),
};

export default slotApi;
