import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Capacitor config — wraps the SwasthyaVaani React PWA as a native Android app (one codebase).
 * Native audio capture and connectivity come from plugins; the same build serves web and app.
 *
 * For local dev against a LAN backend over http, point the app at the dev server with
 * CAP_SERVER_URL and allow cleartext (see docs/android.md) — production stays https-only.
 */
const config: CapacitorConfig = {
  appId: 'ai.swasthyavaani.app',
  appName: 'SwasthyaVaani',
  webDir: 'dist',
  android: {
    // App traffic to the in-India backend is https/wss in production (CLAUDE.md §7.2).
    allowMixedContent: false,
  },
  server: {
    androidScheme: 'https',
    ...(process.env.CAP_SERVER_URL
      ? { url: process.env.CAP_SERVER_URL, cleartext: true }
      : {}),
  },
};

export default config;
