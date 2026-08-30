import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/resumeAnalyser": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false
      },
      "/resumeAnalyserCore": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false
      }
    }
  }
})