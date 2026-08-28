# SwasthyaVaani 🩺🎙️

**Voice-first, offline-capable field documentation for India's frontline health workers — built on Sarvam AI's Indic speech stack and AWS.**

> *Swasthya* (health) + *Vaani* (voice). A health worker just talks; the record writes itself.

![status](https://img.shields.io/badge/status-proof--of--concept-orange)
![stack](https://img.shields.io/badge/stack-Sarvam%20AI%20%2B%20Spring%20Boot%20%2B%20AWS-blue)
![license](https://img.shields.io/badge/license-MIT-green)

---

## The problem

India's ~1 million ASHA and ANM frontline health workers spend a large part of every day on paperwork — logging home visits, maternal and child checkups, immunization status, and referrals into registers and government apps. In practice they:

- are semi-literate in English, yet the forms are English-first,
- speak naturally in **code-mixed dialect** (e.g. *"baby-r weight thik aache, kintu fever holo kal theke"*),
- work in villages with **little or no connectivity**.

The result is delayed, lossy, error-prone health data — the exact data the public-health system depends on.

## The solution

The worker narrates the visit in her own language. SwasthyaVaani:

1. **transcribes** the speech live (code-mixed, Indian-accent),
2. **extracts** a structured visit record — patient, weight, symptoms, next visit date,
3. **reads it back** for confirmation in her language, and
4. **syncs** it to the backend registry when a signal returns — with no data lost and nothing duplicated.

It works fully offline and queues visits until connectivity is available.

## Why Sarvam AI

This is solvable *specifically* because of Sarvam's India-first stack — capabilities global models handle poorly:

- **Code-mixed STT** tuned for Indian accents and dialects (how ASHA workers actually speak).
- **Realtime streaming transcription** (`saaras:v3-realtime`) for a responsive speak-and-see experience.
- **Native-script OCR** (Sarvam Vision) to ingest existing paper registers, not just new entries.
- **In-India data residency**, which is non-negotiable for health PII and a clean compliance story.

The engineering value-add on top is the **offline-first, sync-later architecture** and disciplined PII handling.

---

## Architecture

```mermaid
flowchart LR
    subgraph Field["📱 Field (offline-capable PWA)"]
        A[Voice capture] --> B[Local queue<br/>IndexedDB + Service Worker]
    end

    subgraph AWS["☁️ AWS ap-south-1 (Mumbai)"]
        C[Spring Boot API<br/>WebFlux]
        D[(DynamoDB<br/>visit records)]
        E[(S3<br/>audio + OCR)]
        F[SQS<br/>sync queue]
    end

    subgraph Sarvam["🧠 Sarvam AI (in-India)"]
        G[Saaras v3-realtime · STT]
        H[Sarvam Vision · OCR]
        I[sarvam-105b · extraction]
        J[Sarvam-Translate]
        K[Bulbul · TTS]
    end

    B -- WSS / sync --> C
    C <--> G
    C --> H
    C --> I
    C --> J
    C --> K
    C --> F --> D
    C --> E
    K -- readback --> A
```

### Pipeline → Sarvam model mapping

| Step | Sarvam model / endpoint | Purpose |
|---|---|---|
| Live speech capture | `saaras:v3-realtime` (WebSocket) | Streaming, code-mixed STT |
| Batch / fallback STT | Saaras v3 (`transcribe`) | When realtime is unavailable |
| Paper-record ingest | Sarvam Vision (OCR) | Native-script OCR of registers |
| Field extraction | `sarvam-105b` | Transcript → structured record |
| Official-record text | Sarvam-Translate | Formal registry entry |
| Confirmation readback | Bulbul (TTS) | Speak record back in-language |

---

## Tech stack

- **Frontend:** React 18 + TypeScript, Vite, PWA (Workbox), IndexedDB — offline-first, runs on low-end Android.
- **Backend:** Java 21 + Spring Boot 3.x (WebFlux) — reactive streaming to Sarvam, orchestration, sync/reconcile.
- **AI:** Sarvam AI (Saaras, Bulbul, Sarvam-Translate, sarvam-105b, Sarvam Vision).
- **Data:** DynamoDB (records), S3 (audio/OCR), SQS (sync queue).
- **Infra:** AWS CDK (TypeScript), region `ap-south-1`.

---

## Getting started

### Prerequisites

- Java 21, Maven
- Node 20+, pnpm
- An AWS account (for Phase 5 deploy) and AWS CDK
- A Sarvam AI API key — https://docs.sarvam.ai

### Setup

```bash
git clone <your-repo-url> swasthya-vaani && cd swasthya-vaani

# configure secrets (never commit these)
cp backend/.env.example backend/.env       # add SARVAM_API_KEY etc.
cp frontend/.env.example frontend/.env

# backend
cd backend && ./mvnw spring-boot:run

# frontend (new terminal)
cd frontend && pnpm install && pnpm dev
```

Open the printed local URL, install the PWA, and try a recording. See `CLAUDE.md` for the full command list and build phases.

---

## Project structure

```
swasthya-vaani/
├── CLAUDE.md              # build guidance for Claude Code
├── README.md
├── docs/                  # architecture, Sarvam integration, data model
├── infra/                 # AWS CDK (TypeScript)
├── backend/               # Spring Boot service
├── frontend/              # React PWA
└── scripts/               # dev / setup / seed
```

---

## Roadmap

- [ ] **Phase 0** — Repo scaffold, `SarvamClient` abstraction, health checks
- [ ] **Phase 1** — Online happy path: record → STT → extraction → structured record
- [ ] **Phase 2** — Bulbul TTS readback + edit-and-correct
- [ ] **Phase 3** — Realtime STT + full offline queue and idempotent sync
- [ ] **Phase 4** — Sarvam Vision OCR ingest of paper records
- [ ] **Phase 5** — CDK deploy to `ap-south-1` + runbook

---

## Data & privacy

- All processing and storage stays **in India** (`ap-south-1`); Sarvam processes in-India by design.
- Every visit record is treated as sensitive PII: encrypted at rest and in transit, never logged, never sent outside the in-India Sarvam endpoint.
- The PoC uses **synthetic data only** — no real patient information.

## Disclaimer

SwasthyaVaani is a **proof-of-concept**, not a certified medical device. It records and structures what a health worker dictates; it does **not** diagnose, advise on treatment, or make clinical decisions.

## License

MIT — see [LICENSE](./LICENSE).

---

*Built to explore India-first AI: Sarvam's Indic speech models applied to a real frontline problem, with production-grade cloud architecture around them.*
