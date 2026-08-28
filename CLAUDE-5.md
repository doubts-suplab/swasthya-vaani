# CLAUDE.md

Guidance for Claude Code when working in this repository. Read this fully before writing any code.

---

## 1. What this project is

**SwasthyaVaani** ("health voice") is a voice-first, offline-capable field-documentation tool for India's frontline health workers (ASHA / ANM). A worker narrates a home visit in her own language — including code-mixed dialect (e.g. Bengali-English) — and the system:

1. transcribes the speech live,
2. extracts a structured visit record (patient, weight, symptoms, next visit date, etc.),
3. reads the record back for confirmation in her language, and
4. syncs it to a backend registry when connectivity is available.

This is a **proof-of-concept / portfolio project**, not a certified clinical system. See the constraints in §7.

The core thesis: this problem is solvable specifically because of **Sarvam AI's Indic-first speech stack** (code-mixed STT, Indian-accent TTS, native-script OCR, in-India data residency) — capabilities that global models handle poorly. The engineering value-add is the **offline-first, sync-later architecture** over unreliable rural connectivity, plus disciplined PII handling.

---

## 2. Architecture (target)

```
[React PWA client]  --WebSocket-->  [Spring Boot API]  -->  [Sarvam AI APIs]
   |  offline queue                     |                       (STT / TTS / Translate / LLM / OCR)
   |  (IndexedDB +                      |
   |   service worker)                  +-->  SQS (sync queue) --> worker --> DynamoDB (records)
   |                                    +-->  S3 (audio + OCR artifacts)
   v
 sync-later reconciliation
```

- **Client**: installable PWA that works fully offline. Captures audio, holds a local queue of unsynced visits, and reconciles when online.
- **Backend**: Spring Boot (Java 21) orchestration layer. Proxies Sarvam realtime STT over WebSocket, runs the extraction/validation agent, generates TTS readback, and manages the sync/reconcile flow.
- **Data plane**: DynamoDB for visit records, S3 for audio and OCR blobs, SQS for the sync queue.
- **Region**: AWS `ap-south-1` (Mumbai) — see data-sovereignty constraint in §7.

Keep a living copy of this diagram and any deviations in `docs/architecture.md`.

---

## 3. Tech stack

| Layer | Choice | Notes |
|---|---|---|
| Frontend | React 18 + TypeScript, Vite, PWA (Workbox), IndexedDB (via `idb`) | Must run on low-end Android in a browser; offline-first is non-negotiable |
| Backend | Java 21 + Spring Boot 3.x, Spring WebFlux (reactive) | Reactive for streaming STT WebSocket; use `WebClient` for Sarvam REST |
| AI | Sarvam AI APIs | See §6 for model mapping. **Never hardcode model behavior — verify against live docs.** |
| Data | DynamoDB, S3, SQS | Single-table design for DynamoDB where sensible |
| IaC | AWS CDK (TypeScript) | Reuse patterns the author already knows from prior AWS/CDK work |
| Build | Maven (backend), pnpm (frontend) | |
| Tests | JUnit 5 + Testcontainers (backend), Vitest + Playwright (frontend) | |

**Rationale for Java/Spring on the backend:** the author is a Java/Spring Boot + AWS architect. Most AI demos are Python; a robust Spring-based AI integration is the intended differentiator. Do **not** rewrite the backend in Python. A small Python sidecar is acceptable *only* if a specific task genuinely can't be done from Java, and only after flagging it.

---

## 4. Repository structure

```
swasthya-vaani/
├── CLAUDE.md              # this file
├── README.md
├── docs/
│   ├── architecture.md    # living architecture + decisions (ADR-lite)
│   ├── sarvam-integration.md  # verified API contracts, model choices, cost notes
│   └── data-model.md      # visit record schema + DynamoDB design
├── infra/                 # AWS CDK (TypeScript)
├── backend/               # Spring Boot service
│   ├── src/main/java/ai/swasthyavaani/...
│   ├── src/test/java/...
│   └── pom.xml
├── frontend/              # React PWA
│   ├── src/
│   └── package.json
└── scripts/               # dev/setup/seed scripts
```

Create this skeleton in Phase 0 (§9). Don't scatter files outside it.

---

## 5. Conventions

- **Java**: Google Java Format, package root `ai.swasthyavaani`, constructor injection only (no field `@Autowired`), records for DTOs, no business logic in controllers.
- **TypeScript**: strict mode on, functional components + hooks, no `any` without a `// reason:` comment.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`). One logical change per commit.
- **Config**: everything environment-specific via env vars / Spring config — never commit secrets. Provide `.env.example` and `application-example.yml`.
- **Errors**: fail loud in dev, degrade gracefully in the field. Every Sarvam call needs timeout + retry-with-backoff + a defined offline fallback.
- **Docs**: when you make a non-obvious architectural decision, append a short entry to `docs/architecture.md` (context → decision → consequence).

---

## 6. Sarvam AI integration — read carefully

The Sarvam API surface is evolving (the realtime STT API is recent). **Before implementing any Sarvam call, verify the current contract against the live docs** at `https://docs.sarvam.ai` (the Context7 MCP or a web fetch can pull current docs). Do not rely on memorized signatures.

Model → step mapping:

| Pipeline step | Sarvam model / endpoint | Purpose |
|---|---|---|
| Live speech capture | `saaras:v3-realtime` (WebSocket) | Streaming, code-mixed STT with partial transcripts |
| Batch / fallback STT | Saaras v3 (`transcribe` mode) | When realtime socket is unavailable |
| Paper-record ingest | Sarvam Vision (OCR) | Native-script OCR of existing registers/MCP cards |
| Field extraction + validation | `sarvam-105b` (chat/completions) | Turn transcript into structured visit record; the reasoning/agent layer |
| Official-record text | Sarvam-Translate | Formal-language registry entry |
| Confirmation readback | Bulbul (text-to-speech) | Speak the record back in the worker's language |

Rules:
- **Abstract the vendor.** All Sarvam access goes through a `SarvamClient` interface with a single implementation. No Sarvam SDK calls leaking into controllers or UI.
- **Cost + quota awareness.** STT/TTS are metered. Don't stream audio you don't need; debounce; cache TTS for repeated confirmations. Note rough per-call cost assumptions in `docs/sarvam-integration.md`.
- **Language handling.** Default target languages: Bengali + English (code-mixed). Make the language set config-driven so other states can be added.
- **No real patient data.** Use synthetic transcripts/fixtures for all tests and demos.

---

## 7. Hard constraints (guardrails — do not violate)

1. **Data sovereignty.** All processing and storage stays in India (`ap-south-1`). Sarvam already processes in-India; the AWS side must match. Never introduce a dependency that ships PII outside India.
2. **PII discipline.** Treat every visit record as sensitive. Encrypt at rest (S3 SSE, DynamoDB encryption) and in transit (TLS/WSS). No PII in logs, no PII in commit history, no PII in prompts sent anywhere except the in-India Sarvam endpoint.
3. **Offline-first is a feature, not a nice-to-have.** The app must let a worker complete and queue a visit with zero connectivity, then reconcile later without data loss or duplicates (idempotent sync keyed by a client-generated UUID).
4. **Not a medical device.** No diagnosis, no dosage advice, no clinical decision-making. The system records what the worker says; it does not advise.
5. **Graceful degradation.** If Sarvam realtime is down, fall back to record-then-batch-transcribe. If Sarvam is fully unreachable, still capture and queue raw audio + manual entry.

If a requested change would break one of these, stop and flag it rather than implementing it.

---

## 8. Commands

Fill these in as the scaffold is built; keep this section accurate.

```bash
# Backend
cd backend && ./mvnw spring-boot:run          # run locally
./mvnw test                                    # unit + integration tests
./mvnw verify                                  # full build + checks

# Frontend
cd frontend && pnpm install
pnpm dev                                        # dev server
pnpm test                                        # Vitest
pnpm build                                        # production PWA build

# Infra
cd infra && pnpm install
pnpm cdk diff
pnpm cdk deploy
```

---

## 9. Build phases (build incrementally, don't try to do everything at once)

- **Phase 0 — Scaffold.** Repo skeleton (§4), `.env.example`, `SarvamClient` interface + config, health-check endpoints, CI stub. No features yet.
- **Phase 1 — Happy-path online.** Record audio → batch STT → `sarvam-105b` extraction → structured record shown on screen. Online only.
- **Phase 2 — Readback loop.** Bulbul TTS confirmation + edit-and-correct flow.
- **Phase 3 — Realtime + offline.** Swap to `saaras:v3-realtime` streaming; add IndexedDB queue, service worker, idempotent sync via SQS → DynamoDB.
- **Phase 4 — OCR ingest.** Sarvam Vision to import an existing paper record into the same schema.
- **Phase 5 — Deploy.** CDK stack for `ap-south-1`; wire S3/DynamoDB/SQS; document runbook.

At the end of each phase: tests green, `README` + `docs` updated, a working demo path.

**Definition of done for the PoC:** a worker can, on a low-end phone with no signal, narrate a visit in Bengali-English, get a confirmed structured record, and have it sync cleanly once back online — with no PII leaving India and no data lost.

---

## 10. Working style for Claude Code

- Prefer small, reviewable changes over large sweeping ones. Show a plan before large refactors.
- When the Sarvam contract is uncertain, check the live docs first; state the assumption if docs are ambiguous.
- Write the test alongside the code, not after.
- Keep `docs/` in sync with reality — future-you and the author both rely on it.
- Ask before adding a new heavyweight dependency or a second language runtime.
