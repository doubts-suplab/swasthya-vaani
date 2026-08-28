# Data Model

Canonical schema for a **visit record** — the structured output of the extraction step and the unit that syncs to the backend. This is the contract that ties the Sarvam extraction step, the client offline queue, and DynamoDB together. Keep it authoritative: if the schema changes, bump `schemaVersion` (§9) and update this file first.

---

## 1. Principles

- **The record captures what the worker dictated, not a diagnosis.** No clinical interpretation, no dosage advice. (See the "not a medical device" constraint in `CLAUDE.md`.)
- **Client-generated `visitId` (UUID v4) is the idempotency key.** It is created on the device the moment recording starts, survives offline, and is what makes sync exactly-once.
- **Unknown means `null`, never a guess.** The extraction model must not invent values.
- **PII stays minimal and in-India.** No Aadhaar or government ID numbers in the PoC — use a local `beneficiaryRef`. Free-text transcripts and audio live in S3, not in DynamoDB.
- **Every record is auditable.** Provenance fields record how it was produced (source language, models, confidence) so a human can trust or correct it.

---

## 2. Canonical visit record (example)

```json
{
  "visitId": "8f2c1a44-6b0e-4c9a-9f1d-2e7a5b3c1d90",
  "schemaVersion": 1,
  "workerId": "ASHA-WB-24PGS-00417",
  "deviceId": "dev-3a9f",
  "visitType": "ANC",
  "visitTimestamp": "2026-08-28T09:14:00+05:30",
  "location": {
    "villageName": "Amtala",
    "block": "Bishnupur",
    "district": "South 24 Parganas",
    "state": "WB"
  },
  "beneficiary": {
    "beneficiaryRef": "BEN-24PGS-1093",
    "name": "Rekha Das",
    "category": "PREGNANT_WOMAN",
    "gender": "FEMALE",
    "ageYears": 24
  },
  "observations": {
    "weightKg": 52.5,
    "temperatureC": 37.8,
    "bloodPressure": { "systolic": 118, "diastolic": 76 },
    "gestationWeeks": 28,
    "reportedSymptoms": ["fever since yesterday", "mild swelling in feet"],
    "notes": "Advised rest and fluids; iron tablets handed over."
  },
  "actions": {
    "medicinesHandedOver": ["IFA tablets"],
    "referral": {
      "referred": true,
      "facility": "Bishnupur PHC",
      "urgency": "ROUTINE",
      "reason": "fever review"
    },
    "nextVisitDate": "2026-09-11"
  },
  "provenance": {
    "sourceLanguage": "bn-en",
    "sttModel": "saaras:v3-realtime",
    "extractionModel": "sarvam-105b",
    "extractionConfidence": 0.86,
    "warnings": ["temperatureC inferred from 'thora jor' — confirm with worker"],
    "audioS3Key": "audio/2026/08/28/8f2c1a44.opus",
    "transcriptS3Key": "transcripts/2026/08/28/8f2c1a44.txt"
  },
  "confirmationStatus": "CONFIRMED",
  "syncStatus": "SYNCED",
  "createdOffline": true,
  "createdAt": "2026-08-28T09:14:00+05:30",
  "updatedAt": "2026-08-28T09:16:22+05:30",
  "syncedAt": "2026-08-28T11:02:10+05:30"
}
```

---

## 3. Field reference

PII class: **H** = high (direct identifier), **M** = medium (quasi-identifier), **L** = low / non-personal.

| Field | Type | Required | PII | Notes |
|---|---|---|---|---|
| `visitId` | UUID v4 (string) | yes | L | Client-generated; idempotency key |
| `schemaVersion` | int | yes | L | See §9 |
| `workerId` | string | yes | M | Health-worker identifier |
| `deviceId` | string | yes | L | For debugging offline sync |
| `visitType` | enum | yes | L | See §4 |
| `visitTimestamp` | ISO-8601 w/ offset | yes | L | When the visit happened |
| `location.villageName` | string | yes | M | |
| `location.block` / `district` | string | no | M | |
| `location.state` | enum (state code) | yes | L | e.g. `WB` |
| `beneficiary.beneficiaryRef` | string | yes | M | Local ref — **never Aadhaar** |
| `beneficiary.name` | string | yes | **H** | Encrypt; never log |
| `beneficiary.category` | enum | yes | L | See §4 |
| `beneficiary.gender` | enum | no | L | |
| `beneficiary.ageYears` | int | no | M | Prefer age over DOB |
| `observations.weightKg` | number | no | L | kg |
| `observations.temperatureC` | number | no | L | °C |
| `observations.bloodPressure` | `{systolic,diastolic}` int | no | L | mmHg |
| `observations.gestationWeeks` | int | no | L | ANC only |
| `observations.reportedSymptoms` | string[] | no | M | Worker's words, not diagnosis |
| `observations.notes` | string | no | M | Free text |
| `actions.medicinesHandedOver` | string[] | no | L | What was physically given |
| `actions.referral` | object | no | L | See example |
| `actions.referral.urgency` | enum | no | L | Worker-flagged, not clinical |
| `actions.nextVisitDate` | ISO date | no | L | |
| `provenance.sourceLanguage` | string | yes | L | BCP-47-ish; `bn-en` = code-mixed |
| `provenance.sttModel` | string | yes | L | Model + version |
| `provenance.extractionModel` | string | yes | L | Model + version |
| `provenance.extractionConfidence` | number 0–1 | yes | L | |
| `provenance.warnings` | string[] | no | L | Low-confidence / inferred fields |
| `provenance.audioS3Key` | string | no | M | Pointer only; blob in S3 |
| `provenance.transcriptS3Key` | string | no | M | Pointer only; text in S3 |
| `confirmationStatus` | enum | yes | L | See §4 |
| `syncStatus` | enum | yes | L | See §4 |
| `createdOffline` | bool | yes | L | |
| `createdAt` / `updatedAt` | ISO-8601 | yes | L | |
| `syncedAt` | ISO-8601 | no | L | Set on successful sync |

---

## 4. Enums / controlled vocabulary

| Enum | Values |
|---|---|
| `visitType` | `ANC`, `PNC`, `IMMUNIZATION`, `CHILD_GROWTH`, `GENERAL`, `OTHER` |
| `beneficiary.category` | `PREGNANT_WOMAN`, `LACTATING_MOTHER`, `INFANT`, `CHILD_UNDER_5`, `ADULT`, `OTHER` |
| `gender` | `FEMALE`, `MALE`, `OTHER` |
| `referral.urgency` | `ROUTINE`, `URGENT`, `EMERGENCY` |
| `confirmationStatus` | `DRAFT` (extracted, unconfirmed), `CONFIRMED` (worker approved), `EDITED` (worker changed a field) |
| `syncStatus` | `PENDING`, `SYNCED`, `FAILED`, `SUPERSEDED` |

Keep enums config-driven where they'll vary by state/program; validate on both client and backend.

---

## 5. Extraction contract (what `sarvam-105b` must emit)

The extraction step receives the transcript (+ minimal context: worker's default location, expected language) and must return **strict JSON only** — no prose, no markdown fences — matching the `observations`, `beneficiary`, `actions`, and a `warnings` array. Rules the prompt must enforce:

1. Output valid JSON only.
2. Any field not clearly stated in the transcript → `null` (or omitted array). **Never fabricate.**
3. When a value is inferred rather than explicit, still fill it but add a human-readable note to `warnings`.
4. Numbers are numbers, not strings; dates are ISO-8601.
5. No diagnosis, no medication *advice* — only medicines the worker says were handed over.

JSON Schema (extraction subset) for validation:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "VisitExtraction",
  "type": "object",
  "additionalProperties": false,
  "required": ["beneficiary", "observations", "actions", "warnings"],
  "properties": {
    "beneficiary": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "name": { "type": ["string", "null"] },
        "category": { "enum": ["PREGNANT_WOMAN","LACTATING_MOTHER","INFANT","CHILD_UNDER_5","ADULT","OTHER", null] },
        "gender": { "enum": ["FEMALE","MALE","OTHER", null] },
        "ageYears": { "type": ["integer", "null"], "minimum": 0, "maximum": 120 }
      }
    },
    "observations": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "weightKg": { "type": ["number", "null"] },
        "temperatureC": { "type": ["number", "null"] },
        "bloodPressure": {
          "type": ["object", "null"],
          "properties": {
            "systolic": { "type": "integer" },
            "diastolic": { "type": "integer" }
          }
        },
        "gestationWeeks": { "type": ["integer", "null"], "minimum": 0, "maximum": 45 },
        "reportedSymptoms": { "type": "array", "items": { "type": "string" } },
        "notes": { "type": ["string", "null"] }
      }
    },
    "actions": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "medicinesHandedOver": { "type": "array", "items": { "type": "string" } },
        "referral": {
          "type": ["object", "null"],
          "properties": {
            "referred": { "type": "boolean" },
            "facility": { "type": ["string", "null"] },
            "urgency": { "enum": ["ROUTINE","URGENT","EMERGENCY", null] },
            "reason": { "type": ["string", "null"] }
          }
        },
        "nextVisitDate": { "type": ["string", "null"], "format": "date" }
      }
    },
    "warnings": { "type": "array", "items": { "type": "string" } }
  }
}
```

The backend wraps this extraction with `visitId`, `workerId`, `provenance`, timestamps, and sync fields to form the full record. Validate every extraction against this schema before persisting; on failure, keep the visit as `DRAFT` and surface the raw transcript for manual entry.

---

## 6. DynamoDB design (single-table)

Table `swasthyavaani` (PoC-scale single-table; adjust for real load later).

| | Partition key (`PK`) | Sort key (`SK`) |
|---|---|---|
| Base table | `WORKER#{workerId}` | `VISIT#{visitTimestamp}#{visitId}` |
| GSI1 (by beneficiary) | `BEN#{beneficiaryRef}` | `VISIT#{visitTimestamp}` |
| GSI2 (by visitId / dedup) | `VISIT#{visitId}` | `VISIT#{visitId}` |
| GSI3 (reconcile queue) | `SYNC#{syncStatus}` | `VISIT#{visitTimestamp}` |

Access patterns:

| # | Need | Query |
|---|---|---|
| 1 | Visits for a worker, newest first | base table, `PK = WORKER#..`, `SK begins_with VISIT#`, scan-forward false |
| 2 | Full history for a beneficiary | GSI1, `PK = BEN#..` |
| 3 | Look up / dedup a single visit by id | GSI2, `PK = VISIT#{visitId}` |
| 4 | Backend list of failed/pending syncs | GSI3, `PK = SYNC#FAILED` |

Encryption at rest is on (AWS-managed KMS minimum). `beneficiary.name` and free-text fields are the sensitive ones — keep them out of GSI keys.

---

## 7. Idempotency & offline sync

- The device assigns `visitId` at record start and stores the record in IndexedDB with `syncStatus = PENDING`.
- On sync, the client sends the record; the backend performs an **idempotent upsert** keyed on `visitId` (conditional write / check GSI2). Re-sending the same `visitId` is a no-op success — this is what tolerates flaky connectivity and retries.
- If a worker edits a record that was already synced, the client bumps `updatedAt` and re-syncs; last-writer-wins by `updatedAt`. The superseded server copy (if any conflict logic is added later) is marked `SUPERSEDED`.
- Audio/transcript upload to S3 is a separate idempotent step keyed by the same `visitId`-derived S3 key; the record can sync before or after the blob.

---

## 8. Artifact storage (S3 layout)

```
s3://swasthyavaani-artifacts-apsouth1/
├── audio/{yyyy}/{MM}/{dd}/{visitId}.opus
├── transcripts/{yyyy}/{MM}/{dd}/{visitId}.txt
└── ocr/{yyyy}/{MM}/{dd}/{visitId}-page{n}.json   # Sarvam Vision output (Phase 4)
```

SSE enabled, bucket private, TLS-only bucket policy, lifecycle rules to be defined at deploy.

---

## 9. Schema versioning

- `schemaVersion` starts at `1`. Bump on any breaking change to record shape.
- Backend must accept the current version and one prior; migrations documented here.
- Update this file **before** the code when the schema changes, and note the change in `docs/architecture.md`.

---

## 10. Open questions (resolve as the PoC matures)

- Coded vocabularies for symptoms/medicines (free text now — map to a standard later if needed).
- Whether coarse GPS is worth capturing vs. village name only (privacy trade-off).
- Multi-beneficiary visits (one household, several people) — single record with array vs. one record each.
