import axiosInstance from './axiosInstance';

export const authApi = {
  login: (credentials) =>
    axiosInstance.post('/auth/login', credentials),

  register: (userData) =>
    axiosInstance.post('/auth/register', userData),

  logout: () =>
    axiosInstance.post('/auth/logout'),

  refreshToken: (refreshToken) =>
    axiosInstance.post('/auth/refresh', { refreshToken }),

  getProfile: () =>
    axiosInstance.get('/auth/profile'),
};

export default authApi;
