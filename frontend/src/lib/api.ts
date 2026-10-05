import axios, { AxiosRequestConfig } from 'axios';
import toast from 'react-hot-toast';
import { useAuthStore } from '@/store/authStore';

// Extend config to allow silent requests (no toast on error)
export interface SilentConfig extends AxiosRequestConfig {
  silent?: boolean;
}

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: attach JWT ──
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor: handle 401 / errors ──
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as SilentConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = useAuthStore.getState().refreshToken;

      if (refreshToken) {
        try {
          const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken });
          const { accessToken, refreshToken: newRefreshToken, user } = data.data;
          // ✅ Store the NEW refresh token (old one is revoked by backend)
          useAuthStore.getState().setAuth(accessToken, newRefreshToken, user);
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        } catch {
          useAuthStore.getState().logout();
          window.location.href = '/login';
        }
      } else {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      }
    }

    // Show error toast for non-auth errors UNLESS the request is marked as silent
    if (error.response?.status !== 401 && !originalRequest.silent) {
      const message = error.response?.data?.message || 'Something went wrong';
      toast.error(message);
    }

    return Promise.reject(error);
  }
);

export default api;
