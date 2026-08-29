import { CfnOutput, Duration, RemovalPolicy, Stack, type StackProps } from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import type { Construct } from 'constructs';
import { assertInIndia } from './residency';

/**
 * The SwasthyaVaani data plane, pinned to `ap-south-1`:
 *  - DynamoDB single-table (`data-model.md` §6) with 3 GSIs, encrypted, PITR on
 *  - S3 artifacts bucket (audio / transcripts / OCR) — SSE, TLS-only, private, versioned
 *  - SQS sync queue + DLQ for the idempotent reconcile pipeline
 *
 * Encryption at rest and in transit is on everywhere; nothing is public. See `docs/runbook.md`.
 */
export class DataPlaneStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // Residency guard — fail synth if this stack is not in India (T7-F04).
    assertInIndia(props.env?.region);

    // --- DynamoDB single-table -------------------------------------------
    const table = new dynamodb.Table(this, 'VisitsTable', {
      tableName: 'swasthyavaani',
      partitionKey: { name: 'PK', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'SK', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      encryption: dynamodb.TableEncryption.AWS_MANAGED,
      pointInTimeRecoverySpecification: { pointInTimeRecoveryEnabled: true },
      removalPolicy: RemovalPolicy.RETAIN, // never auto-delete PII
    });
    // GSI1 — by beneficiary; GSI2 — by visitId (dedup); GSI3 — reconcile-by-syncStatus.
    for (const n of [1, 2, 3]) {
      table.addGlobalSecondaryIndex({
        indexName: `GSI${n}`,
        partitionKey: { name: `GSI${n}PK`, type: dynamodb.AttributeType.STRING },
        sortKey: { name: `GSI${n}SK`, type: dynamodb.AttributeType.STRING },
      });
    }

    // --- S3 artifacts bucket ---------------------------------------------
    const artifacts = new s3.Bucket(this, 'ArtifactsBucket', {
      bucketName: 'swasthyavaani-artifacts-apsouth1',
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      enforceSSL: true, // deny non-TLS access
      versioned: true,
      removalPolicy: RemovalPolicy.RETAIN,
      lifecycleRules: [
        {
          transitions: [
            { storageClass: s3.StorageClass.INFREQUENT_ACCESS, transitionAfter: Duration.days(90) },
          ],
          noncurrentVersionExpiration: Duration.days(365),
        },
      ],
    });

    // --- SQS sync queue + DLQ --------------------------------------------
    const dlq = new sqs.Queue(this, 'SyncDlq', {
      queueName: 'swasthyavaani-sync-dlq',
      encryption: sqs.QueueEncryption.SQS_MANAGED,
      retentionPeriod: Duration.days(14),
    });
    const syncQueue = new sqs.Queue(this, 'SyncQueue', {
      queueName: 'swasthyavaani-sync',
      encryption: sqs.QueueEncryption.SQS_MANAGED,
      visibilityTimeout: Duration.seconds(300),
      deadLetterQueue: { queue: dlq, maxReceiveCount: 5 },
    });

    // --- Outputs (consumed by the API's config) --------------------------
    new CfnOutput(this, 'TableName', { value: table.tableName });
    new CfnOutput(this, 'ArtifactsBucketName', { value: artifacts.bucketName });
    new CfnOutput(this, 'SyncQueueUrl', { value: syncQueue.queueUrl });
    new CfnOutput(this, 'Region', { value: this.region });
  }
}
