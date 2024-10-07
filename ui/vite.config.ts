import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react()],
    base: '/ssh-script-runner',
    server: {
        proxy: {
            '^/ssh-script-runner/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
    build: {
        outDir: '../src/main/resources/static',
        emptyOutDir: true,
    },
})
