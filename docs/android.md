# Android app (Capacitor)

SwasthyaVaani ships as an installable Android app **without a parallel codebase**: the same
React PWA (`frontend/`) is wrapped with [Capacitor](https://capacitorjs.com), which gives the
web app a native shell plus native plugins (audio capture, connectivity). One codebase, two
runtimes. See the decision rationale in `architecture.md` (ADR-008).

> Why not native Kotlin? For a PoC, a second native codebase doubles the UI surface for little
> gain. Capacitor keeps a single React app while still getting native audio and a Play-Store
> artifact. A true native app remains an option later (it would reuse this backend and the
> `VisitRecord` JSON Schema).

---

## Layout

```
frontend/
├── capacitor.config.ts     # app id, name, webDir=dist, server config
├── src/hooks/useVoiceCapture.ts   # native (plugin) OR web (MediaRecorder) capture — one API
└── android/                # the generated native Android project (committed)
    └── app/src/main/AndroidManifest.xml   # RECORD_AUDIO etc.
```

- **App id:** `ai.swasthyavaani.app`  ·  **App name:** SwasthyaVaani
- Copied web assets, generated config, and build outputs under `android/` are git-ignored
  (Capacitor's own `.gitignore`); the native project itself is committed.

## How capture works across runtimes

`useVoiceCapture()` exposes one interface (`start / stop / recording / status`) and picks the
implementation at runtime:

| Runtime | Implementation | Audio format |
|---|---|---|
| Native Android | `capacitor-voice-recorder` plugin (real permission prompt, reliable on low-end) | AAC (`audio/aac`) |
| Web / PWA | `MediaRecorder` | WebM/Opus |

The upload path maps the MIME type to a filename extension the Sarvam STT accepts (Saaras
handles AAC/M4A/WebM/OGG/WAV), so the backend is unchanged.

## Prerequisites for building the APK

The web-side integration and the `android/` project are ready. Producing an APK additionally
needs a local **Android SDK** (not required to develop the web app, and not present in CI web
sessions):

- JDK 21 (already used by the backend), Gradle 8.11+ (the project ships a Gradle wrapper).
- Android SDK (via Android Studio or command-line tools) with `ANDROID_HOME` /
  `local.properties` set to the SDK path.

## Commands

```bash
cd frontend
pnpm build            # build the web assets (dist/)
pnpm cap:sync         # copy web assets + plugins into android/
pnpm android:open     # open in Android Studio (recommended for first run/emulator)

# or a headless debug build (needs Android SDK on PATH):
pnpm android:build    # build + sync + ./gradlew assembleDebug
# -> android/app/build/outputs/apk/debug/app-debug.apk
```

## Pointing the app at your backend

The packaged app loads the bundled web assets and calls the API at `VITE_API_BASE_URL`
(baked in at `pnpm build`). Two common setups:

- **Device/emulator → backend on your machine:** build the web assets with
  `VITE_API_BASE_URL=https://<your-lan-ip-or-tunnel>` so the app reaches a real, TLS backend.
- **Live-reload dev:** set `CAP_SERVER_URL=http://<lan-ip>:5173` before `cap sync` to load the
  Vite dev server directly (this enables cleartext for dev only — production stays https, per
  `CLAUDE.md` §7.2).

Never ship an APK pointing at a cleartext/production-PII endpoint over http; the data-residency
and PII rules (`CLAUDE.md` §7) apply to the app exactly as to the web client.

## Permissions

`AndroidManifest.xml` declares `INTERNET`, `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, and
`ACCESS_NETWORK_STATE`. The voice-recorder plugin requests microphone permission at runtime the
first time a worker records.
