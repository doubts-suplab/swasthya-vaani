package ai.swasthyavaani.api.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AWS data-plane configuration ({@code aws.*}), bound only under the {@code aws} profile. Region
 * defaults to — and is enforced as — India ({@code ap-south-1}, CLAUDE.md §7.1). {@code
 * dynamoEndpoint} lets local integration tests point at DynamoDB Local / LocalStack.
 */
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
    @DefaultValue("ap-south-1") String region,
    @DefaultValue("swasthyavaani") String tableName,
    String dynamoEndpoint) {}
