# Progress Tracker

**Single live source of truth** for feature status. Feature IDs and definitions live in
[`roadmap.md`](./roadmap.md); this file tracks *state*. Update the row (status, date, note)
whenever work starts or finishes.

**Status legend:** ⬜ Not started · 🟨 In progress · ✅ Done · ⛔ Blocked · ➖ Deferred

**Last updated:** 2026-08-28

---

## Snapshot

| Phase | Done | In progress | Not started | Total |
|---|---|---|---|---|
| **P0 — Scaffold** | 9 | 0 | 0 | 9 |
| **P1 — Online happy path** | 6 | 0 | 0 | 6 |
| **P2 — Readback loop** | 4 | 0 | 0 | 4 |
| **P3 — Realtime + offline** | 4 | 0 | 3 | 7 |
| **P4 — OCR ingest** | 0 | 0 | 3 | 3 |
| **P5 — Deploy** | 0 | 0 | 4 | 4 |
| **Continuous (T7)** | 1 | 0 | 2 | 3 |
| **Mobile / Android (T8)** | 2 | 0 | 2 | 4 |

**Current focus:** Phase 3 core done (offline queue + idempotent sync + realtime proxy) → the remaining P3 items (DynamoDB / SQS / S3 provisioning) are infra and land with **Phase 5 (Deploy)**. Next: **Phase 4 (OCR)** or Phase 5.

---

## Phase 0 — Scaffold

| ID | Feature | Track | Status | Updated | Note |
|---|---|---|---|---|---|
| T1-F01 | Repo skeleton (§4) | T1 | ✅ | 2026-08-28 | `backend/ frontend/ infra/ scripts/ docs/` created |
| T1-F02 | Multi-module Maven (`domain`/`sarvam`/`api`) + wrapper | T1 | ✅ | 2026-08-28 | Reactor builds; `./mvnw` present |
| T1-F03 | `.env.example` + `application-example.yml`, config-driven | T1 | ✅ | 2026-08-28 | No secrets; languages/models in config |
| T1-F04 | CI stub (backend + frontend) | T1 | ✅ | 2026-08-28 | `.github/workflows/ci.yml` |
| T2-F01 | `SarvamClient` interface + WebClient impl skeleton | T2 | ✅ | 2026-08-28 | Vendor seam in `swasthyavaani-sarvam` |
| T3-F01 | Health/readiness (Actuator + `/api/v1/ping`) | T3 | ✅ | 2026-08-28 | Actuator + ping controller |
| T4-F01 | React + TS + Vite PWA skeleton | T4 | ✅ | 2026-08-28 | Installable shell, offline manifest |
| T7-F01 | Domain records + enums mirror `data-model.md` | T7 | ✅ | 2026-08-28 | `ai.swasthyavaani.domain.*` |
| T7-F02 | Baseline test harness green | T7 | ✅ | 2026-08-28 | JUnit 5 + Vitest smoke tests |

---

## Phase 1 — Happy-path online

| ID | Feature | Track | Status | Updated | Note |
|---|---|---|---|---|---|
| T2-F02 | Batch STT `saaras:v3` behind client | T2 | ✅ | 2026-08-28 | `transcribe()` used by pipeline; MockWebServer contract test |
| T2-F03 | Extraction `sarvam-m` → `VisitExtraction` | T2 | ✅ | 2026-08-28 | `extractVisit()` with fence-stripping; contract test |
| T7-F03 | Extraction JSON-Schema validation | T7 | ✅ | 2026-08-28 | `VisitExtractionValidator` vs `data-model.md` §5; invalid → DRAFT |
| T3-F02 | `POST /api/v1/visits/transcribe` | T3 | ✅ | 2026-08-28 | Reactive multipart → `VisitDraftResponse`; 502 on upstream fail |
| T4-F02 | Record → API → render record | T4 | ✅ | 2026-08-28 | MediaRecorder capture → upload → `VisitRecordView` |
| T2-F04 | Code-mixed extraction prompt | T2 | ✅ | 2026-08-28 | Bengali-English + controlled-vocab system prompt |

---

## Phase 2 — Readback loop

| ID | Feature | Track | Status | Updated | Note |
|---|---|---|---|---|---|
| T2-F05 | TTS `bulbul:v3` + cache | T2 | ✅ | 2026-08-28 | `VisitReadbackService` with Caffeine cache keyed by normalised text |
| T3-F03 | Readback endpoint + status transitions | T3 | ✅ | 2026-08-28 | `/readback` + `/confirm`; `VisitConfirmationService` state machine, 409 on superseded |
| T4-F03 | Play readback + inline edit | T4 | ✅ | 2026-08-28 | `VisitReview`: editable fields, play TTS, confirm/edit |
| T2-F06 | Sarvam-Translate registry text | T2 | ✅ | 2026-08-28 | Official text translated for readback; degrades to English on failure |

---

## Phase 3 — Realtime + offline

| ID | Feature | Track | Status | Updated | Note |
|---|---|---|---|---|---|
| T2-F07 | Proxy `saaras:v3-realtime` WSS | T2 | ✅ | 2026-08-28 | `/ws/stt` proxy, key server-side; frame protocol **pending live verification** (sarvam-integration §4) |
| T4-F04 | IndexedDB queue + service worker | T4 | ✅ | 2026-08-28 | `visitQueue` (idb) + Workbox SW; visit completes fully offline |
| T4-F05 | Reconcile-on-reconnect | T4 | ✅ | 2026-08-28 | `syncEngine.drainQueue` + `useSyncQueue`; drains on reconnect, no loss |
| T5-F01 | DynamoDB single-table + GSIs | T5 | ⬜ | — | Infra → **Phase 5**; `VisitRepository` seam ready |
| T5-F02 | Idempotent upsert on `visitId` | T5 | ✅ | 2026-08-28 | `VisitRepository`/`VisitSyncService` last-writer-wins; in-memory now, DynamoDB in P5 |
| T5-F03 | SQS sync queue + worker | T5 | ⬜ | — | Infra → **Phase 5** |
| T5-F04 | S3 artifact upload | T5 | ⬜ | — | Infra → **Phase 5** |

---

## Phase 4 — OCR ingest

| ID | Feature | Track | Status | Updated | Note |
|---|---|---|---|---|---|
| T2-F08 | Sarvam Vision OCR | T2 | ⬜ | — | |
| T3-F04 | OCR → `VisitExtraction` mapping | T3 | ⬜ | — | |
| T4-F06 | Photo-capture + review UI | T4 | ⬜ | — | |

---

## Phase 5 — Deploy

| ID | Feature | Track | Status | Updated | Note |
|---|---|---|---|---|---|
| T6-F01 | CDK: DynamoDB+S3+SQS in `ap-south-1` | T6 | ⬜ | — | |
| T6-F02 | API deploy + secrets wiring | T6 | ⬜ | — | |
| T6-F03 | Runbook + residency/PII checklist | T6 | ⬜ | — | |
| T7-F04 | Residency guard in CI | T7 | ⬜ | — | |

---

## Mobile / Android (Track T8)

| ID | Feature | Status | Updated | Note |
|---|---|---|---|---|
| T8-F01 | Capacitor shell + `android/` project | ✅ | 2026-08-28 | Cap 7; `cap doctor` clean; appId `ai.swasthyavaani.app` |
| T8-F02 | Native-aware voice capture | ✅ | 2026-08-28 | `useVoiceCapture`: plugin on device, MediaRecorder on web |
| T8-F03 | APK build + distribution runbook | ⬜ | — | Web side ready; needs Android SDK — see `docs/android.md` |
| T8-F04 | Native offline storage + connectivity | ⬜ | — | Aligns with Phase 3 queue |

---

## Continuous (Track T7)

| ID | Feature | Status | Updated | Note |
|---|---|---|---|---|
| T7-F05 | No-PII-in-logs enforcement | ⬜ | — | |
| T7-F06 | Observability (correlation id, metrics) | ⬜ | — | |
| T7-F07 | Synthetic fixture library | ✅ | 2026-08-28 | `scripts/fixtures/synthetic-transcripts.json` (code-mixed); grow over time |

---

## Changelog

- **2026-08-28** — **Phase 3 core landed (offline-first + realtime proxy).** Backend:
  `VisitRepository`/`InMemoryVisitRepository` + `VisitSyncService` (idempotent upsert, last-writer-
  wins), `POST /visits/sync` + `GET /visits/{id}`, and a `/ws/stt` realtime proxy that keeps the
  Sarvam key server-side (frame protocol pending live verification). Frontend: IndexedDB
  `visitQueue`, `syncEngine.drainQueue`, `useSyncQueue` (reconcile-on-reconnect), on-device
  confirm. 29 backend + 16 frontend tests green. DynamoDB/SQS/S3 provisioning deferred to Phase 5.
- **2026-08-28** — **Android shell (Capacitor) added (T8-F01/F02).** The React PWA is wrapped as
  a native Android app (Capacitor 7, `frontend/android/`) — one codebase. `useVoiceCapture`
  selects the native voice-recorder plugin on device and MediaRecorder on web; manifest declares
  RECORD_AUDIO. APK build documented in `docs/android.md` (needs Android SDK). No backend change.
- **2026-08-28** — **Phase 2 (readback loop) landed.** Backend: `VisitReadbackService`
  (compose official text → Sarvam-Translate → Bulbul TTS, Caffeine-cached, degrades to English),
  `VisitConfirmationService` state machine, `/readback` + `/confirm` endpoints, 409 on invalid
  transition. Frontend: `VisitReview` — inline field editing, play readback (base64→audio), and
  confirm/edit. 20 backend + 10 frontend tests green.
- **2026-08-28** — **Phase 1 (online happy path) landed.** Backend: `VisitTranscriptionService`
  orchestrating STT → extraction → schema validation → `DRAFT` `VisitRecord`, `POST
  /api/v1/visits/transcribe` (reactive multipart), `VisitExtractionValidator`, RFC-7807 error
  handling, injectable `Clock`. Frontend: MediaRecorder capture hook, visit API client, and the
  `VisitRecordView` review UI. 10 backend + 7 frontend tests green.
- **2026-08-28** — Phase 0 scaffold landed: docs (architecture, Sarvam integration,
  roadmap, progress), multi-module Maven backend (domain/sarvam/api) with `SarvamClient`
  seam and health checks, React PWA skeleton, CI stub.
