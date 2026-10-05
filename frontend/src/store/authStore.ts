import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  organizationId: string;
  organizationName: string;
  permissions: string[];
}

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  setAuth: (token: string, refreshToken: string, user: AuthUser) => void;
  logout: () => void;
  hasPermission: (permission: string) => boolean;
  isCustomer: () => boolean;
  isAgent: () => boolean;
  isAdmin: () => boolean;
  isOwner: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      refreshToken: null,
      user: null,

      setAuth: (token, refreshToken, user) => set({ token, refreshToken, user }),

      logout: () => set({ token: null, refreshToken: null, user: null }),

      hasPermission: (permission: string) => {
        const user = get().user;
        if (!user) return false;
        if (user.role === 'OWNER' || user.role === 'ADMIN') return true;
        return user.permissions.includes(permission);
      },

      isCustomer: () => get().user?.role === 'CUSTOMER',

      isAgent: () => get().user?.role === 'AGENT',

      isAdmin: () => {
        const role = get().user?.role;
        return role === 'ADMIN' || role === 'OWNER';
      },

      isOwner: () => get().user?.role === 'OWNER',
    }),
    {
      name: 'helpdesk-auth',
      partialize: (state) => ({
        token: state.token,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
);
