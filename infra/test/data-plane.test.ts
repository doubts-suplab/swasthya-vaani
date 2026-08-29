import { App } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { describe, expect, it } from 'vitest';
import { DataPlaneStack } from '../lib/data-plane-stack';
import { assertInIndia, INDIA_REGION } from '../lib/residency';

function template(region = INDIA_REGION): Template {
  const app = new App();
  const stack = new DataPlaneStack(app, 'TestStack', {
    env: { account: '123456789012', region },
  });
  return Template.fromStack(stack);
}

describe('DataPlaneStack', () => {
  it('creates an encrypted, PITR-enabled single table with 3 GSIs', () => {
    const t = template();
    t.hasResourceProperties('AWS::DynamoDB::Table', {
      TableName: 'swasthyavaani',
      SSESpecification: { SSEEnabled: true },
      PointInTimeRecoverySpecification: { PointInTimeRecoveryEnabled: true },
      GlobalSecondaryIndexes: Match.arrayWith([
        Match.objectLike({ IndexName: 'GSI1' }),
        Match.objectLike({ IndexName: 'GSI2' }),
        Match.objectLike({ IndexName: 'GSI3' }),
      ]),
    });
  });

  it('locks the S3 bucket down: encrypted, private, TLS-only', () => {
    const t = template();
    t.hasResourceProperties('AWS::S3::Bucket', {
      BucketEncryption: Match.objectLike({ ServerSideEncryptionConfiguration: Match.anyValue() }),
      PublicAccessBlockConfiguration: {
        BlockPublicAcls: true,
        BlockPublicPolicy: true,
        IgnorePublicAcls: true,
        RestrictPublicBuckets: true,
      },
    });
    // enforceSSL adds a bucket policy denying aws:SecureTransport = false
    t.hasResourceProperties('AWS::S3::BucketPolicy', {
      PolicyDocument: Match.objectLike({
        Statement: Match.arrayWith([
          Match.objectLike({
            Effect: 'Deny',
            Condition: { Bool: { 'aws:SecureTransport': 'false' } },
          }),
        ]),
      }),
    });
  });

  it('creates a sync queue with a dead-letter queue', () => {
    const t = template();
    t.resourceCountIs('AWS::SQS::Queue', 2);
    t.hasResourceProperties('AWS::SQS::Queue', {
      QueueName: 'swasthyavaani-sync',
      RedrivePolicy: Match.objectLike({ maxReceiveCount: 5 }),
    });
  });
});

describe('residency guard (T7-F04)', () => {
  it('accepts ap-south-1', () => {
    expect(assertInIndia('ap-south-1')).toBe('ap-south-1');
  });

  it('rejects any non-India region', () => {
    expect(() => assertInIndia('us-east-1')).toThrow(/residency/i);
    expect(() => assertInIndia(undefined)).toThrow(/residency/i);
  });

  it('fails to synthesize a stack outside India', () => {
    expect(() => template('eu-west-1')).toThrow(/residency/i);
  });
});
