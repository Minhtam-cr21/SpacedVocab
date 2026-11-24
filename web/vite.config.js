import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server: http://localhost:5173, API calls are proxied to the API container (port 8090).
// In Docker, nginx serves the built files and proxies /api to the `app` service instead.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8090',
    },
  },
});
