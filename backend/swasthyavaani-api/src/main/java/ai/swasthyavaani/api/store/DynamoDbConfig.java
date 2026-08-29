package ai.swasthyavaani.api.store;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/**
 * Builds the non-blocking DynamoDB client for the {@code aws} profile. Enforces the India-region
 * residency guard at startup (CLAUDE.md §7.1) — the app refuses to talk to DynamoDB anywhere else.
 */
@Configuration
@Profile("aws")
@EnableConfigurationProperties(AwsProperties.class)
public class DynamoDbConfig {

  private static final String INDIA_REGION = "ap-south-1";

  @Bean
  DynamoDbAsyncClient dynamoDbAsyncClient(AwsProperties props) {
    if (!INDIA_REGION.equals(props.region())) {
      throw new IllegalStateException(
          "Data residency violation: aws.region must be '"
              + INDIA_REGION
              + "' (India), got '"
              + props.region()
              + "'.");
    }
    var builder = DynamoDbAsyncClient.builder().region(Region.of(props.region()));
    if (props.dynamoEndpoint() != null && !props.dynamoEndpoint().isBlank()) {
      builder.endpointOverride(URI.create(props.dynamoEndpoint())); // local integration only
    }
    return builder.build();
  }
}
