import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Proxy API calls to Spring Boot during local development.
    // This avoids CORS issues when running React on 5173 and Spring on 8080.
    // In production (Vercel), VITE_API_BASE_URL points directly to Railway.
   proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
 
  build: {
    outDir: 'dist',
    sourcemap: false,
  }
})

