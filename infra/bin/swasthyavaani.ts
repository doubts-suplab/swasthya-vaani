#!/usr/bin/env node
import { App } from 'aws-cdk-lib';
import { ComputeStack } from '../lib/compute-stack';
import { DataPlaneStack } from '../lib/data-plane-stack';
import { INDIA_REGION } from '../lib/residency';

const app = new App();
const env = { account: process.env.CDK_DEFAULT_ACCOUNT, region: INDIA_REGION };

// Region is pinned to India; the account comes from the deploy environment (CDK_DEFAULT_ACCOUNT).
const dataPlane = new DataPlaneStack(app, 'SwasthyaVaaniDataPlane', {
  env,
  description: 'SwasthyaVaani data plane (DynamoDB + S3 + SQS) — ap-south-1',
});

new ComputeStack(app, 'SwasthyaVaaniCompute', {
  env,
  description: 'SwasthyaVaani API (ECS Fargate + ALB) — ap-south-1',
  table: dataPlane.table,
  artifacts: dataPlane.artifacts,
  syncQueue: dataPlane.syncQueue,
});

app.synth();
