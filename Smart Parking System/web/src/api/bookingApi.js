import axiosInstance from './axiosInstance';

export const bookingApi = {
  getBookings: (params) =>
    axiosInstance.get('/bookings', { params }),

  getBookingById: (id) =>
    axiosInstance.get(`/bookings/${id}`),

  createBooking: (bookingData) =>
    axiosInstance.post('/bookings', bookingData),

  cancelBooking: (id) =>
    axiosInstance.patch(`/bookings/${id}/cancel`),

  checkIn: (id) =>
    axiosInstance.patch(`/bookings/${id}/check-in`),

  checkOut: (id) =>
    axiosInstance.patch(`/bookings/${id}/check-out`),
};

export default bookingApi;
