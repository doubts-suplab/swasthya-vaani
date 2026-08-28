# Infra (AWS CDK) — Phase 5

> **Status: not yet scaffolded.** Per the phased plan (`docs/roadmap.md`), the CDK app is
> built in **Phase 5 — Deploy**. This directory holds the plan until then so the data-plane
> design is settled before code lands.

## Planned stack (AWS CDK, TypeScript)

Region is pinned to **`ap-south-1` (Mumbai)** — a hard data-sovereignty constraint
(`CLAUDE.md` §7.1). No resource or dependency may move PII outside India.

| Resource | Purpose | Key settings |
|---|---|---|
| **DynamoDB** table `swasthyavaani` | Visit records (single-table + GSIs) | Encryption at rest (KMS), PITR, on-demand billing; keys per `docs/data-model.md` §6 |
| **S3** `swasthyavaani-artifacts-apsouth1` | Audio, transcripts, OCR blobs | SSE, bucket private, TLS-only bucket policy, lifecycle rules (`data-model.md` §8) |
| **SQS** sync queue (+ DLQ) | Idempotent sync/reconcile pipeline | Consumers keyed by `visitId` |
| **API compute** | Runs the Spring Boot service | ECS Fargate or App Runner in `ap-south-1` (TBD at Phase 5) |
| **Secrets** | `SARVAM_API_KEY` etc. | AWS Secrets Manager; never in code or env files |

## Guardrails to encode here (T7-F04)

- A synth-time assertion / CI check that **every** stack, stack env, and construct targets
  `ap-south-1` and no cross-region/global PII path is introduced.
- Encryption-at-rest on by default for DynamoDB and S3; TLS-only.

## Commands (once scaffolded)

```bash
cd infra
pnpm install
pnpm cdk diff
pnpm cdk deploy
```

See `docs/architecture.md` for the full data-plane diagram and `docs/roadmap.md` (Phase 5)
for the feature breakdown.
