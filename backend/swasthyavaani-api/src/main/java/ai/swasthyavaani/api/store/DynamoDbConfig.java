package ai.swasthyavaani.api.store;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Builds the non-blocking AWS clients (DynamoDB, S3, SQS) for the {@code aws} profile. Enforces the
 * India-region residency guard at startup (CLAUDE.md §7.1) — the app refuses to talk to AWS
 * anywhere else. Credentials come from the default provider chain (an IAM role in deploys).
 */
@Configuration
@Profile("aws")
@EnableConfigurationProperties(AwsProperties.class)
public class DynamoDbConfig {

  private static final String INDIA_REGION = "ap-south-1";

  private static Region indiaRegion(AwsProperties props) {
    if (!INDIA_REGION.equals(props.region())) {
      throw new IllegalStateException(
          "Data residency violation: aws.region must be '"
              + INDIA_REGION
              + "' (India), got '"
              + props.region()
              + "'.");
    }
    return Region.of(props.region());
  }

  @Bean
  DynamoDbAsyncClient dynamoDbAsyncClient(AwsProperties props) {
    var builder = DynamoDbAsyncClient.builder().region(indiaRegion(props));
    if (props.dynamoEndpoint() != null && !props.dynamoEndpoint().isBlank()) {
      builder.endpointOverride(URI.create(props.dynamoEndpoint())); // local integration only
    }
    return builder.build();
  }

  @Bean
  S3AsyncClient s3AsyncClient(AwsProperties props) {
    return S3AsyncClient.builder().region(indiaRegion(props)).build();
  }

  @Bean
  SqsAsyncClient sqsAsyncClient(AwsProperties props) {
    return SqsAsyncClient.builder().region(indiaRegion(props)).build();
  }
}
