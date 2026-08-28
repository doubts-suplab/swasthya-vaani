# Roadmap

How SwasthyaVaani is built — organised as **phases** (temporal milestones, from
`CLAUDE.md` §9) crossed with **tracks** (long-lived workstreams). Each feature has a
stable ID (`T#-F##`) used by [`progress.md`](./progress.md) so status is tracked in one
place and referenced everywhere.

- **Phases** answer *when / in what order* (P0 → P5).
- **Tracks** answer *what kind of work* (a feature belongs to exactly one track).
- Definition of done for the PoC (`CLAUDE.md` §9): a worker can, on a low-end phone with
  no signal, narrate a visit in Bengali-English, get a confirmed structured record, and
  have it sync cleanly once back online — **no PII leaving India, no data lost.**

---

## Tracks

| ID | Track | Owns |
|---|---|---|
| **T1** | Platform & Scaffold | Repo skeleton, multi-module build, CI, config, dev ergonomics |
| **T2** | Voice & AI (Sarvam) | `SarvamClient`, STT, extraction, TTS, translate, OCR, prompts |
| **T3** | Backend Services | WebFlux endpoints, orchestration, validation, sync/reconcile |
| **T4** | Frontend PWA | Capture UI, offline queue, readback/edit loop, installability |
| **T5** | Data & Sync | Visit-record schema, DynamoDB, S3, SQS, idempotent sync |
| **T6** | Infra & Deploy | AWS CDK, `ap-south-1` wiring, runbook |
| **T7** | Quality & Security | Tests, PII discipline, residency guardrails, observability |
| **T8** | Mobile (Android) | Capacitor shell, native capture, APK build & distribution |

---

## Phase 0 — Scaffold  *(no features yet)*

Goal: a clean, buildable skeleton with the vendor seam and health checks in place.

| ID | Feature | Track | DoD |
|---|---|---|---|
| **T1-F01** | Repo skeleton per `CLAUDE.md` §4 (`backend/`, `frontend/`, `infra/`, `scripts/`, `docs/`) | T1 | Structure exists; nothing scattered outside it |
| **T1-F02** | Multi-module Maven backend (`domain`, `sarvam`, `api`) + wrapper | T1 | `./mvnw -q verify` builds all modules |
| **T1-F03** | `.env.example` + `application-example.yml`; config-driven languages & models | T1 | No secrets committed; app boots with example config |
| **T1-F04** | CI stub (backend build + frontend build) | T1 | Workflow file present and runnable |
| **T2-F01** | `SarvamClient` interface + single WebClient impl skeleton + `SarvamProperties` | T2 | Interface compiles; impl wired behind config; no SDK leakage |
| **T3-F01** | Health/readiness endpoints (Actuator + `/api/v1/ping`) | T3 | `GET` returns healthy JSON |
| **T4-F01** | React + TS + Vite PWA skeleton (installable shell) | T4 | `pnpm build` produces a PWA; app shell renders |
| **T7-F01** | Domain model as Java records + enums mirroring `data-model.md` | T7 | Types compile; enums match schema §4 |
| **T7-F02** | Baseline test harness (JUnit 5 / Vitest) green | T7 | `./mvnw test` and `pnpm test` pass |

---

## Phase 1 — Happy-path online

Goal: record → batch STT → extraction → structured record on screen (online only).

| ID | Feature | Track | DoD |
|---|---|---|---|
| **T2-F02** | Batch STT via `saaras:v3` (`transcribe`/`codemix`) behind `SarvamClient` | T2 | Audio → transcript, contract-tested with fixtures |
| **T2-F03** | Extraction via `sarvam-m` chat/completions → `VisitExtraction` | T2 | Transcript → strict JSON |
| **T7-F03** | Extraction JSON-Schema validation (`data-model.md` §5); invalid → `DRAFT` | T7 | Bad JSON never persisted; raw transcript surfaced |
| **T3-F02** | `POST /api/v1/visits/transcribe` orchestration endpoint | T3 | Returns a `DRAFT` `VisitRecord` |
| **T4-F02** | Record audio → call API → render structured record | T4 | Worker sees the parsed record |
| **T2-F04** | Prompt engineering for code-mixed Bengali-English extraction | T2 | Fixtures extract correctly; no fabrication |

---

## Phase 2 — Readback loop

Goal: spoken confirmation + edit-and-correct.

| ID | Feature | Track | DoD |
|---|---|---|---|
| **T2-F05** | TTS via `bulbul:v3`; cache by normalised text | T2 | Readback audio returned; cache hit on repeat |
| **T3-F03** | Readback endpoint + `confirmationStatus` transitions (`DRAFT→CONFIRMED/EDITED`) | T3 | State machine enforced server-side |
| **T4-F03** | Play readback + inline edit of any field | T4 | Worker confirms/edits; record updates |
| **T2-F06** | Sarvam-Translate for the formal registry-entry text | T2 | Official-language string produced |

---

## Phase 3 — Realtime + offline

Goal: streaming STT and the full offline queue with idempotent sync.

| ID | Feature | Track | DoD |
|---|---|---|---|
| **T2-F07** | Proxy `saaras:v3-realtime` WSS (key stays server-side) | T2 | Live partial transcripts through the API |
| **T4-F04** | IndexedDB visit queue + Workbox service worker | T4 | Full visit completes with zero connectivity |
| **T4-F05** | Reconcile-on-reconnect (drain queue, mark `SYNCED`) | T4 | Queue drains; no dupes, no loss |
| **T5-F01** | DynamoDB single-table + GSIs (`data-model.md` §6) | T5 | Access patterns 1–4 served |
| **T5-F02** | Idempotent upsert keyed on `visitId` (conditional write via GSI2) | T5 | Re-send is a no-op success |
| **T5-F03** | SQS sync queue + worker (consumes → DynamoDB) | T5 | Async sync path works idempotently |
| **T5-F04** | S3 audio/transcript upload, idempotent by `visitId` key | T5 | Blob + record reconcile in any order |

---

## Phase 4 — OCR ingest

Goal: import an existing paper record into the same schema.

| ID | Feature | Track | DoD |
|---|---|---|---|
| **T2-F08** | Sarvam Vision OCR of registers/MCP cards | T2 | Native-script page → text |
| **T3-F04** | OCR → `VisitExtraction` mapping + validation | T3 | Paper record → structured `DRAFT` |
| **T4-F06** | Photo-capture + review UI for OCR ingest | T4 | Worker photographs, reviews, confirms |

---

## Phase 5 — Deploy

Goal: CDK stack for `ap-south-1`, wired data plane, runbook.

| ID | Feature | Track | DoD |
|---|---|---|---|
| **T6-F01** | CDK app: DynamoDB + S3 + SQS in `ap-south-1`, encryption on | T6 | `cdk diff` clean; resources in-region |
| **T6-F02** | API deploy target + config/secrets wiring | T6 | Backend runs against real data plane |
| **T6-F03** | Runbook + residency/PII checklist in `docs/` | T6 | Ops can deploy & verify guardrails |
| **T7-F04** | Residency guard: CI/asserts fail on any non-`ap-south-1` / non-India dependency | T7 | Guardrail §7.1 enforced automatically |

---

## Mobile — Android (Track T8)

Wraps the React PWA as a native Android app via Capacitor — one codebase, no parallel app
(`docs/android.md`, ADR-008).

| ID | Feature | Status DoD |
|---|---|---|
| **T8-F01** | Capacitor shell + `android/` project (app id, manifest, plugins) | `cap doctor` clean; web build syncs into the app |
| **T8-F02** | Native-aware voice capture (`useVoiceCapture`: plugin on device, MediaRecorder on web) | One capture API; native records AAC, web WebM |
| **T8-F03** | APK build + distribution runbook (needs Android SDK) | `pnpm android:build` documented; signing/Play notes |
| **T8-F04** | Native offline storage + connectivity (align with Phase 3 queue) | Queue persists via native storage; `@capacitor/network` status |

---

## Cross-phase / continuous (Track T7)

| ID | Feature | DoD |
|---|---|---|
| **T7-F05** | No-PII-in-logs enforcement (log scrubbing + review) | Sensitive fields never logged |
| **T7-F06** | Observability: correlation id, Actuator metrics, health indicators | Trace a visit end-to-end |
| **T7-F07** | Synthetic fixture library (code-mixed transcripts) | Reused across all AI tests |

---

## How to update

1. When work starts/finishes on a feature, update its row in [`progress.md`](./progress.md)
   (status + date + note) — that file is the single live source of truth.
2. If scope changes, edit the feature here and keep its ID stable.
3. Record non-obvious architectural decisions as ADR-lite entries in
   [`architecture.md`](./architecture.md) §7.
