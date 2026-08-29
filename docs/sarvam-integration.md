# Sarvam AI Integration

Verified vendor contracts, model choices, and cost notes for every Sarvam capability the
system uses. **All access goes through the `SarvamClient` interface** (`CLAUDE.md` §6) —
no SDK calls leak into controllers or UI.

> **Verification status.** Contracts below were verified against Sarvam's live docs via
> the Context7 documentation index on **2026-08-28**. The realtime STT WebSocket surface
> is the newest and most likely to drift — re-verify before implementing Phase 3. Where a
> value is an assumption, it is marked **(assumption — verify)**.

---

## 1. Connection basics

| | Value |
|---|---|
| Base URL | `https://api.sarvam.ai` |
| Auth header | `api-subscription-key: <SARVAM_API_KEY>` (WS header casing: `Api-Subscription-Key`) |
| Alt auth | `Authorization: Bearer <key>` is also accepted; the docs/SDKs prefer the subscription-key header |
| Transport | HTTPS for REST, WSS for realtime; **in-India processing** (satisfies residency §7.1) |
| Content types | STT: `multipart/form-data`; TTS/Chat/Translate: `application/json` |

The key is supplied via env/config only (`SARVAM_API_KEY`), never committed. See
`backend/.env.example` and `application-example.yml`.

---

## 2. Capability → model mapping (reconciled)

`CLAUDE.md` §6 names capabilities; the concrete model ids below are what the **live API**
exposes (Aug 2026). The id is always a config value under `sarvam.models.*`.

| Pipeline step | CLAUDE.md name | **Verified live id** | Endpoint |
|---|---|---|---|
| Live speech capture | `saaras:v3-realtime` | `saaras:v3-realtime` | `wss://api.sarvam.ai/speech-to-text/ws` |
| Batch / fallback STT | Saaras v3 | `saaras:v3` | `POST /speech-to-text` |
| Field extraction | `sarvam-105b` | **`sarvam-m`** (assumption: `sarvam-105b` is a legacy/internal alias — verify) | `POST /v1/chat/completions` |
| Official-record text | Sarvam-Translate | `sarvam-translate:v1` **(assumption — verify)** | `POST /translate` |
| Confirmation readback | Bulbul (TTS) | `bulbul:v3` | `POST /text-to-speech` |
| Paper-record ingest | Sarvam Vision (OCR) | Sarvam Vision / Doc-parse **(assumption — verify at Phase 4)** | Vision endpoint |

> ⚠️ **Open item for the author:** confirm whether `sarvam-105b` is a real, available model
> id on your account or an internal codename. The code defaults to `sarvam-m`; change
> `sarvam.models.extraction` in config if `sarvam-105b` is the intended production model.
> This is logged as **ADR-003** in `architecture.md`.

---

## 3. Speech-to-Text (batch / fallback) — `POST /speech-to-text`

- **Method / content:** `POST`, `multipart/form-data`.
- **Headers:** `api-subscription-key`.
- **Form fields:**
  - `file` *(required, binary)* — WAV, MP3, AAC, AIFF, OGG, OPUS, FLAC, MP4/M4A, AMR, WMA,
    WebM, PCM. For PCM (`pcm_s16le`, `pcm_l16`, `pcm_raw`) set `input_audio_codec`; PCM is
    16 kHz only. Best for clips **< 30 s** (use the Batch API for longer files).
  - `model` *(required)* — `saaras:v3` (recommended) or `saarika:v2.5` (legacy).
  - `mode` *(optional, saaras:v3 only)* — `transcribe` (default) | `translate` |
    `verbatim` | `translit` | `codemix`.
  - `language_code` *(optional)* — BCP-47 (e.g. `bn-IN`, `hi-IN`, `en-IN`); `unknown` for
    auto-detect.
- **200 response:**
  ```json
  {
    "request_id": "20260209_abc123-...",
    "transcript": "নমস্তে, আপনি কেমন আছেন?",
    "timestamps": null,
    "diarized_transcript": null,
    "language_code": "bn-IN",
    "language_probability": 0.95
  }
  ```
- **Error codes:** 400, 403, 422, 429 (quota), 500, 503 (overloaded). Retry 429/503 with
  backoff; do not retry 400/422.
- **For code-mixed Bengali-English**, `mode=codemix` (or `transcribe` with
  `language_code=bn-IN`) matches how ASHA workers actually speak. Confirm empirically with
  synthetic fixtures.

---

## 4. Realtime streaming STT — `wss://api.sarvam.ai/speech-to-text/ws`

- **Model:** `saaras:v3-realtime` (only this id is accepted on the realtime channel).
- **Auth:** `Api-Subscription-Key` header, or browser subprotocol
  `api-subscription-key.<key>`.
- **Query params:** `language-code`, `model` (default `saaras:v3`), `mode`
  (default `transcribe`), `sample_rate` (default `16000`),
  `positive_speech_threshold` (default `0.7`), `negative_speech_threshold`
  (default `0.45`).
- **Semantics:** streams true partial transcripts with mid-call reconfiguration. There is
  a sibling `speech-to-text-translate` channel that folds in English translation.
- **Backend role:** the Spring Boot API **proxies** this socket (client ⇄ API ⇄ Sarvam) so
  the subscription key never reaches the browser and audio stays server-mediated.
- **(assumption — verify)** exact channel path, message framing (binary audio chunks +
  JSON control frames), and event schema before Phase 3.

> **Implementation status (Phase 3):** the backend proxy is built — `/ws/stt` relays frames to this
> URL with the key in the `Api-Subscription-Key` header (`SarvamRealtimeProxyHandler`,
> `RealtimeUri`; URI/key-safety unit-tested). The **frame protocol above is still unverified against
> live Sarvam**, so the batch STT path (§3) remains the supported route until a live check confirms
> the framing. See `architecture.md` ADR-010.

---

## 5. Text-to-Speech (Bulbul) — `POST /text-to-speech`

- **Method / content:** `POST`, `application/json`.
- **Body:**
  ```json
  {
    "text": "শিশুর ওজন ঠিক আছে। পরের ভিজিট এগারোই সেপ্টেম্বর।",
    "target_language_code": "bn-IN",
    "model": "bulbul:v3",
    "speaker": "shubh",
    "speech_sample_rate": 22050
  }
  ```
  - `text` *(required)* — code-mixed allowed; limit 2500 chars (`bulbul:v3`).
  - `target_language_code` *(required)* — BCP-47.
  - `speaker` *(optional)* — default `shubh` (v3); 30+ voices.
  - `speech_sample_rate` *(optional)* — 8000/16000/22050/24000/32000/44100/48000; default
    22050.
  - `pace` (0.5–2.0, v3); `enable_preprocessing` (normalise English/numbers).
- **Response:** `{ "request_id": "...", "audios": ["<base64-wav-chunk>", ...] }` — **join
  the chunks and base64-decode before playback/storage.**
- **Cost control:** TTS is metered → **cache readback audio keyed by the normalised text**
  so repeated confirmations don't re-synthesize.

---

## 6. Field extraction (chat/completions) — `POST /v1/chat/completions`

- **Method / content:** `POST`, `application/json` (OpenAI-compatible shape).
- **Body:**
  ```json
  {
    "model": "sarvam-m",
    "messages": [
      { "role": "system", "content": "<extraction system prompt — strict JSON only>" },
      { "role": "user", "content": "<transcript + minimal context>" }
    ],
    "temperature": 0.1,
    "top_p": 1
  }
  ```
- **Response:** OpenAI-style `choices[0].message.content` carrying the JSON string.
- **Contract the model must emit:** the `VisitExtraction` subset in `data-model.md` §5 —
  **strict JSON only**, unknown → `null`, never fabricate, inferred values still filled but
  noted in `warnings`. The backend validates every extraction against the JSON Schema
  (`data-model.md` §5) before persisting; on failure the visit stays `DRAFT` and the raw
  transcript is surfaced for manual entry.
- Keep `temperature` low for determinism. Prompt lives in the `sarvam` module, not inline
  in a controller.

---

## 7. Translation (official-record text) — `POST /translate`

- Produces the formal-language registry entry from the worker's dictated content.
- **(assumption — verify)** exact field names (`input`, `source_language_code`,
  `target_language_code`, `model`) and limits before Phase 1+ wiring.

---

## 8. OCR — Sarvam Vision (Phase 4)

- Native-script OCR of existing paper registers / MCP cards into the same
  `VisitExtraction` schema.
- **(assumption — verify)** endpoint, request shape, and output structure at Phase 4;
  store raw OCR JSON in S3 (`ocr/{yyyy}/{MM}/{dd}/{visitId}-page{n}.json`).

---

## 9. Resilience policy (applies to every call)

| Concern | Policy |
|---|---|
| Timeouts | connect ≈ 3 s, read ≈ 30 s (STT/TTS can be longer for big clips) — config-driven |
| Retries | bounded (e.g. 3) exponential backoff **with jitter**, only on 429/503/network |
| No-retry | 400/401/403/422 — surface immediately |
| Fallback | see the degradation ladder in `architecture.md` §6 |
| Idempotency | STT/TTS are side-effect-free; safe to retry within budget |

---

## 10. Cost & quota notes (rough, PoC-scale — refine with live pricing)

- STT and TTS are **metered per second/character** and priced differently for REST vs
  Batch; diarization (Batch only) costs extra.
- **Don't stream audio you don't need:** debounce capture, trim silence, prefer one clean
  clip over chatty partials when realtime isn't required.
- **Cache TTS** for repeated confirmations (§5).
- Track rough per-call assumptions here as they're measured; wire real pricing from
  `https://docs.sarvam.ai/api-reference-docs/pricing`.

---

## 11. Testing rule

**No real patient data.** All STT/extraction/TTS tests use synthetic transcripts and
fixtures (e.g. `"baby-r weight thik aache, kintu fever holo kal theke"`). Vendor calls are
mocked/stubbed in unit tests; a thin contract test may hit a sandbox only with a
non-production key and synthetic input.
