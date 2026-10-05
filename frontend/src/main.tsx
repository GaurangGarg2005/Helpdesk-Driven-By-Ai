import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import App from './App';
import '@/styles/global.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2,      // 2 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: {
            background: '#1E293B',
            color: '#F8FAFC',
            border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: '8px',
            fontSize: '14px',
            padding: '12px 16px',
            boxShadow: '0 10px 15px rgba(0,0,0,0.3)',
          },
          success: { iconTheme: { primary: '#22C55E', secondary: '#fff' } },
          error:   { iconTheme: { primary: '#EF4444', secondary: '#fff' } },
        }}
      />
    </QueryClientProvider>
  </React.StrictMode>
);
