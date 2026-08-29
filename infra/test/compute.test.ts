import { App } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { describe, expect, it } from 'vitest';
import { ComputeStack } from '../lib/compute-stack';
import { DataPlaneStack } from '../lib/data-plane-stack';
import { INDIA_REGION } from '../lib/residency';

function computeTemplate(region = INDIA_REGION): Template {
  const app = new App();
  const env = { account: '123456789012', region };
  const data = new DataPlaneStack(app, 'Data', { env });
  const compute = new ComputeStack(app, 'Compute', {
    env,
    table: data.table,
    artifacts: data.artifacts,
    syncQueue: data.syncQueue,
  });
  return Template.fromStack(compute);
}

describe('ComputeStack', () => {
  it('runs a Fargate service with the aws profile and an ECR repo', () => {
    const t = computeTemplate();
    t.resourceCountIs('AWS::ECS::Service', 1);
    t.hasResourceProperties('AWS::ECR::Repository', { RepositoryName: 'swasthyavaani-api' });
    t.hasResourceProperties('AWS::ECS::TaskDefinition', {
      ContainerDefinitions: Match.arrayWith([
        Match.objectLike({
          Environment: Match.arrayWith([
            Match.objectLike({ Name: 'SPRING_PROFILES_ACTIVE', Value: 'aws' }),
          ]),
        }),
      ]),
    });
  });

  it('enforces the India-region residency guard', () => {
    expect(() => computeTemplate('us-east-1')).toThrow(/residency/i);
  });
});
