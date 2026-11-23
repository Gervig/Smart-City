import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The backend has no CORS configuration, so the browser never talks to it directly.
// Every request goes to /backend/... on the Vite server, which forwards it to the API
// and strips the /backend prefix. Change the target with VITE_API_TARGET if needed.
const target = process.env.VITE_API_TARGET || 'http://localhost:7070'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/backend': {
        target,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/backend/, ''),
      },
    },
  },
})
