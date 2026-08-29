import { Duration, Stack, type StackProps } from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecsPatterns from 'aws-cdk-lib/aws-ecs-patterns';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import type { Construct } from 'constructs';
import { assertInIndia } from './residency';

export interface ComputeStackProps extends StackProps {
  readonly table: dynamodb.Table;
  readonly artifacts: s3.Bucket;
  readonly syncQueue: sqs.Queue;
}

/**
 * Runs the SwasthyaVaani API as an ECS Fargate service behind an ALB in `ap-south-1`, wired to the
 * data plane (T6-F02). The API runs under the `aws` Spring profile; least-privilege IAM grants only
 * the DynamoDB/S3/SQS access it needs. The container image lives in an ECR repo (push the Spring
 * Boot image there; the placeholder below is swapped for it at deploy — see `docs/runbook.md`).
 */
export class ComputeStack extends Stack {
  constructor(scope: Construct, id: string, props: ComputeStackProps) {
    super(scope, id, props);
    assertInIndia(props.env?.region);

    const repository = new ecr.Repository(this, 'ApiRepository', {
      repositoryName: 'swasthyavaani-api',
      imageScanOnPush: true,
    });

    const vpc = new ec2.Vpc(this, 'Vpc', { maxAzs: 2, natGateways: 1 });
    const cluster = new ecs.Cluster(this, 'Cluster', { vpc });

    const service = new ecsPatterns.ApplicationLoadBalancedFargateService(this, 'ApiService', {
      cluster,
      cpu: 512,
      memoryLimitMiB: 1024,
      desiredCount: 2,
      minHealthyPercent: 50,
      circuitBreaker: { rollback: true },
      healthCheckGracePeriod: Duration.seconds(60),
      taskImageOptions: {
        // Deploy swaps this for repository's pushed image; a public sample keeps synth valid.
        image: ecs.ContainerImage.fromRegistry('public.ecr.aws/docker/library/busybox:latest'),
        containerPort: 8080,
        environment: {
          SPRING_PROFILES_ACTIVE: 'aws',
          AWS_REGION: this.region,
          SWASTHYAVAANI_TABLE: props.table.tableName,
          SWASTHYAVAANI_ARTIFACTS_BUCKET: props.artifacts.bucketName,
          SWASTHYAVAANI_SYNC_QUEUE_URL: props.syncQueue.queueUrl,
        },
      },
    });

    service.targetGroup.configureHealthCheck({ path: '/actuator/health' });

    // Least-privilege access to the data plane.
    props.table.grantReadWriteData(service.taskDefinition.taskRole);
    props.artifacts.grantPut(service.taskDefinition.taskRole);
    props.syncQueue.grantConsumeMessages(service.taskDefinition.taskRole);
    repository.grantPull(service.taskDefinition.executionRole!);
  }
}
