import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  // sockjs-client references Node's `global` variable which doesn't exist in browsers.
  // This polyfill injects it so the WebSocket chat client initialises correctly.
  define: {
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api/v1/auth': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/org': { target: 'http://localhost:8082', changeOrigin: true },
      '/api/v1/tickets':  { target: 'http://localhost:8083', changeOrigin: true },
      '/api/v1/chat':     { target: 'http://localhost:8084', changeOrigin: true },
      '/api/v1/kb':       { target: 'http://localhost:8085', changeOrigin: true },
      '/api/v1/analytics':      { target: 'http://localhost:8086', changeOrigin: true },
      '/api/v1/notifications':  { target: 'http://localhost:8087', changeOrigin: true },
      '/ws/chat':               { target: 'http://localhost:8084', changeOrigin: true, ws: true },
    },
  },
});

