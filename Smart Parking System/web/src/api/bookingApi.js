import axiosInstance from './axiosInstance';

export const bookingApi = {
  getMyBookings: (params) =>
    axiosInstance.get('/bookings/my', { params }),

  getBookings: (params) =>
    axiosInstance.get('/bookings', { params }),

  getBookingById: (id) =>
    axiosInstance.get(`/bookings/${id}`),

  createBooking: (bookingData) =>
    axiosInstance.post('/bookings', bookingData),

  cancelBooking: (id) =>
    axiosInstance.post(`/bookings/${id}/cancel`),

  checkIn: (id) =>
    axiosInstance.post(`/bookings/${id}/check-in`),

  checkOut: (id) =>
    axiosInstance.post(`/bookings/${id}/check-out`),
};

export default bookingApi;
