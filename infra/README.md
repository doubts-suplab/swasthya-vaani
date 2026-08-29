# Infra (AWS CDK) — Phase 5

AWS CDK (TypeScript) for the SwasthyaVaani **data plane**, pinned to **`ap-south-1` (Mumbai)**.
Region residency is a hard constraint (`CLAUDE.md` §7.1) enforced at synth time — see
`lib/residency.ts` (T7-F04). Full deploy steps and the residency/PII checklist are in
[`../docs/runbook.md`](../docs/runbook.md).

## What it provisions

| Resource | Construct | Hardening |
|---|---|---|
| DynamoDB table `swasthyavaani` | `lib/data-plane-stack.ts` | single-table + 3 GSIs (`data-model.md` §6), AWS-managed KMS, PITR, on-demand, RETAIN |
| S3 `swasthyavaani-artifacts-apsouth1` | same | SSE, all public access blocked, TLS-only, versioned, lifecycle, RETAIN |
| SQS `swasthyavaani-sync` (+ DLQ) | same | SSE, redrive to DLQ (maxReceiveCount 5) |

## Commands

```bash
pnpm install
pnpm test            # CDK assertions (Template) + residency guard — runs without AWS creds
pnpm synth           # synthesize CloudFormation
pnpm diff
pnpm deploy          # needs AWS credentials + CDK_DEFAULT_ACCOUNT (see runbook)
```

## Layout

```
infra/
├── bin/swasthyavaani.ts     # CDK app entry (region pinned to ap-south-1)
├── lib/data-plane-stack.ts  # DynamoDB + S3 + SQS
├── lib/residency.ts         # India-region guard (fails synth elsewhere)
└── test/data-plane.test.ts  # assertions + residency-guard tests
```

The API binds to these under the `aws` Spring profile (`DynamoDbVisitRepository`); see the runbook.
