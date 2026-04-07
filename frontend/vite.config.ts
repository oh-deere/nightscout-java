import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // Build directly into Spring Boot's classpath static resources
    outDir: resolve(__dirname, '../target/classes/static'),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8090',
      '/socket.io': 'http://localhost:8090',
    },
  },
})
