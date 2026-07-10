import { create } from 'zustand';
import { authApi } from '../api/authApi';
import { message } from 'antd';

const useAuthStore = create((set, get) => ({
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  token: localStorage.getItem('token') || null,
  isAuthenticated: !!localStorage.getItem('token'),
  loading: false,

  login: async (email, password) => {
    set({ loading: true });
    try {
      const { data } = await authApi.login({ email, password });
      const responseData = data.data || data;
      const token = responseData.accessToken || responseData.token;
      const refreshToken = responseData.refreshToken;

      // Build user object from response fields (backend now returns user info)
      const user = {
        id: responseData.userId,
        email: responseData.email || email,
        role: responseData.role,
        fullName: responseData.fullName,
      };

      localStorage.setItem('token', token);
      if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify(user));

      set({
        user,
        token,
        isAuthenticated: true,
        loading: false,
      });

      message.success('Đăng nhập thành công!');
      return { success: true, role: user.role };
    } catch (error) {
      set({ loading: false });
      const msg = error.response?.data?.message || 'Đăng nhập thất bại';
      message.error(msg);
      return { success: false };
    }
  },

  logout: async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      set({
        user: null,
        token: null,
        isAuthenticated: false,
      });
      message.success('Đã đăng xuất');
    }
  },

  setUser: (user) => {
    localStorage.setItem('user', JSON.stringify(user));
    set({ user });
  },

  checkAuth: () => {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    set({
      token,
      user,
      isAuthenticated: !!token,
    });
  },
}));

export default useAuthStore;
