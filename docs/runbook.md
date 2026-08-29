# Deploy runbook (Phase 5)

How to stand up the SwasthyaVaani data plane in `ap-south-1` and run the API against it. Read
`architecture.md` and `data-model.md` §6 first. **`cdk deploy` requires AWS credentials you supply**
— synth and tests run without them.

---

## 0. Residency & PII checklist (do not skip — CLAUDE.md §7)

- [ ] Region is **`ap-south-1`** everywhere. The CDK app hard-fails synth otherwise
      (`infra/lib/residency.ts`, T7-F04), and the API refuses to start on a non-India region
      (`DynamoDbConfig`).
- [ ] No dependency, bucket, table, or queue is created outside India.
- [ ] Encryption at rest on: DynamoDB (AWS-managed KMS), S3 (SSE), SQS (SSE) — asserted by
      `infra/test`.
- [ ] S3 bucket is private (all public access blocked) and **TLS-only** (bucket policy denies
      `aws:SecureTransport=false`).
- [ ] `SARVAM_API_KEY` and any secret come from **AWS Secrets Manager**, never from committed config.
- [ ] No PII in logs (CLAUDE.md §7.2). `beneficiary.name` and free-text never logged.
- [ ] Synthetic data only in any non-prod environment (CLAUDE.md §6).

---

## 1. Prerequisites

- Node 20+, pnpm; AWS CDK v2 (via `pnpm` in `infra/`).
- AWS credentials for the target account (`aws configure` / SSO), region `ap-south-1`.
- Java 21 + the built API jar (or a container image) for the compute step.

## 2. Provision the data plane (DynamoDB + S3 + SQS)

```bash
cd infra
pnpm install
pnpm test                       # CDK assertions + residency guard
export CDK_DEFAULT_ACCOUNT=<your-account-id>
pnpm cdk bootstrap aws://$CDK_DEFAULT_ACCOUNT/ap-south-1   # first time only
pnpm cdk diff
pnpm cdk deploy                 # creates: swasthyavaani (DynamoDB), artifacts bucket, sync queue+DLQ
```

Note the stack outputs: `TableName`, `ArtifactsBucketName`, `SyncQueueUrl`, `Region`.

## 3. Run the API against DynamoDB

The API uses the in-memory store by default; the **`aws` profile** swaps in DynamoDB
(`DynamoDbVisitRepository`). Credentials come from the standard AWS chain — on ECS/EC2 use an **IAM
role** (never static keys).

```bash
export SPRING_PROFILES_ACTIVE=aws
export AWS_REGION=ap-south-1
export SWASTHYAVAANI_TABLE=swasthyavaani        # matches the stack's TableName output
export SARVAM_API_KEY=...                         # from Secrets Manager in real deploys
cd backend && ./mvnw -pl swasthyavaani-api -am spring-boot:run
```

Minimum IAM for the API role: `dynamodb:PutItem`, `dynamodb:Query` on the table and its
`GSI2`/`GSI3` indexes (extend as read patterns grow); later, `s3:PutObject` on the artifacts bucket
and `sqs:SendMessage` on the sync queue.

### Local integration test (optional, no AWS account)

Point the client at DynamoDB Local / LocalStack and create the table with the same keys/GSIs:

```bash
export SPRING_PROFILES_ACTIVE=aws
# in application-aws.yml, set aws.dynamo-endpoint: http://localhost:8000
```

## 4. Deploy the compute (API)

The API is a container (build the Spring Boot image, push to ECR in `ap-south-1`, run on ECS
Fargate or App Runner **in-region**). Inject `SPRING_PROFILES_ACTIVE=aws`, `AWS_REGION`,
`SWASTHYAVAANI_TABLE`, and secrets via the platform's Secrets Manager integration. Front it with TLS.

> The compute stack (ECR/ECS/ALB) is intentionally not scripted yet — see T6-F02 in
> `docs/roadmap.md`. The data plane and the app's binding to it are the Phase-5 deliverables here.

## 5. Verify

- `GET /actuator/health` → `sarvam` indicator reflects key presence; overall `UP`.
- `POST /api/v1/visits/sync` with a synthetic record, then `GET /api/v1/visits/{visitId}` returns it.
- Re-`POST` the same record → the response `outcome` is `DUPLICATE_IGNORED` and no second item
  appears in DynamoDB (idempotency, `data-model.md` §7).

## 6. Rollback / teardown

- Redeploy a previous image for the API.
- `pnpm cdk destroy` removes the stack **except** the DynamoDB table and S3 bucket (RemovalPolicy
  RETAIN — they hold PII and are kept deliberately). Delete those manually only when you intend to.

---

## Still to do beyond this runbook

- **T5-F03** — SQS is provisioned; wiring the API to enqueue and a worker to drain into DynamoDB
  (async decoupling) is the next step. Today `POST /sync` writes to DynamoDB synchronously and
  idempotently.
- **T5-F04** — the artifacts bucket is provisioned; uploading audio/transcript/OCR blobs (keyed by
  `visitId`, `data-model.md` §8) is the next step.
- **T6-F02** — script the ECR/ECS/ALB compute stack in CDK.
