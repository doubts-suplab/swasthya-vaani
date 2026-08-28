import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

// SwasthyaVaani PWA: installable, offline-first (Workbox service worker).
// Offline-first is non-negotiable (CLAUDE.md §7.3) — the app shell and assets are
// precached so a worker can open and use the app with zero connectivity.
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      manifest: {
        name: 'SwasthyaVaani',
        short_name: 'SwasthyaVaani',
        description: 'Voice-first, offline field documentation for frontline health workers',
        theme_color: '#0f766e',
        background_color: '#ffffff',
        display: 'standalone',
        start_url: '/',
        lang: 'bn-IN',
        icons: [
          { src: 'icons/icon-192.svg', sizes: '192x192', type: 'image/svg+xml', purpose: 'any' },
          { src: 'icons/icon-512.svg', sizes: '512x512', type: 'image/svg+xml', purpose: 'any maskable' },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,woff2}'],
        // API/AI calls are never cached blindly — sync is idempotent and handled
        // explicitly by the offline queue (Phase 3), not by opaque runtime caching.
        navigateFallback: 'index.html',
      },
      devOptions: { enabled: false },
    }),
  ],
  server: { port: 5173 },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
});
