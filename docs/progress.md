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
| **P1 — Online happy path** | 0 | 0 | 6 | 6 |
| **P2 — Readback loop** | 0 | 0 | 4 | 4 |
| **P3 — Realtime + offline** | 0 | 0 | 7 | 7 |
| **P4 — OCR ingest** | 0 | 0 | 3 | 3 |
| **P5 — Deploy** | 0 | 0 | 4 | 4 |
| **Continuous (T7)** | 0 | 0 | 3 | 3 |

**Current focus:** Phase 0 complete → begin Phase 1 (Track T2/T3 online happy path).

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
| T2-F02 | Batch STT `saaras:v3` behind client | T2 | ⬜ | — | |
| T2-F03 | Extraction `sarvam-m` → `VisitExtraction` | T2 | ⬜ | — | |
| T7-F03 | Extraction JSON-Schema validation | T7 | ⬜ | — | |
| T3-F02 | `POST /api/v1/visits/transcribe` | T3 | ⬜ | — | |
| T4-F02 | Record → API → render record | T4 | ⬜ | — | |
| T2-F04 | Code-mixed extraction prompt | T2 | ⬜ | — | |

---

## Phase 2 — Readback loop

| ID | Feature | Track | Status | Updated | Note |
|---|---|---|---|---|---|
| T2-F05 | TTS `bulbul:v3` + cache | T2 | ⬜ | — | |
| T3-F03 | Readback endpoint + status transitions | T3 | ⬜ | — | |
| T4-F03 | Play readback + inline edit | T4 | ⬜ | — | |
| T2-F06 | Sarvam-Translate registry text | T2 | ⬜ | — | |

---

## Phase 3 — Realtime + offline

| ID | Feature | Track | Status | Updated | Note |
|---|---|---|---|---|---|
| T2-F07 | Proxy `saaras:v3-realtime` WSS | T2 | ⬜ | — | |
| T4-F04 | IndexedDB queue + service worker | T4 | ⬜ | — | |
| T4-F05 | Reconcile-on-reconnect | T4 | ⬜ | — | |
| T5-F01 | DynamoDB single-table + GSIs | T5 | ⬜ | — | |
| T5-F02 | Idempotent upsert on `visitId` | T5 | ⬜ | — | |
| T5-F03 | SQS sync queue + worker | T5 | ⬜ | — | |
| T5-F04 | S3 artifact upload | T5 | ⬜ | — | |

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

## Continuous (Track T7)

| ID | Feature | Status | Updated | Note |
|---|---|---|---|---|
| T7-F05 | No-PII-in-logs enforcement | ⬜ | — | |
| T7-F06 | Observability (correlation id, metrics) | ⬜ | — | |
| T7-F07 | Synthetic fixture library | ⬜ | — | |

---

## Changelog

- **2026-08-28** — Phase 0 scaffold landed: docs (architecture, Sarvam integration,
  roadmap, progress), multi-module Maven backend (domain/sarvam/api) with `SarvamClient`
  seam and health checks, React PWA skeleton, CI stub.
