#!/usr/bin/env node
import { App } from 'aws-cdk-lib';
import { DataPlaneStack } from '../lib/data-plane-stack';
import { INDIA_REGION } from '../lib/residency';

const app = new App();

// Region is pinned to India; the account comes from the deploy environment (CDK_DEFAULT_ACCOUNT).
new DataPlaneStack(app, 'SwasthyaVaaniDataPlane', {
  env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: INDIA_REGION },
  description: 'SwasthyaVaani data plane (DynamoDB + S3 + SQS) — ap-south-1',
});

app.synth();
