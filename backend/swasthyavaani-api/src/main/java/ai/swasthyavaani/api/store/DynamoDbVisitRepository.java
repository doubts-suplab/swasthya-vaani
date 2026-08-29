package ai.swasthyavaani.api.store;

import ai.swasthyavaani.domain.model.VisitRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

/**
 * DynamoDB single-table {@link VisitRepository} (active under the {@code aws} profile). Implements
 * the same idempotent contract as the in-memory store using a conditional write for
 * last-writer-wins, and reads a single visit via GSI2 (dedup index) — {@code data-model.md} §6, §7.
 *
 * <p>Non-blocking throughout (async client → {@link Mono}/{@link Flux}). Item mapping is delegated
 * to the unit-tested {@link VisitItem}. This adapter is compile-verified here; integration against
 * DynamoDB Local / a real table is the remaining deploy step (see {@code docs/runbook.md}).
 */
@Repository
@Profile("aws")
public class DynamoDbVisitRepository implements VisitRepository {

  private final DynamoDbAsyncClient client;
  private final AwsProperties props;
  private final VisitItem mapper;

  public DynamoDbVisitRepository(
      DynamoDbAsyncClient client, AwsProperties props, ObjectMapper objectMapper) {
    this.client = client;
    this.props = props;
    this.mapper = new VisitItem(objectMapper);
  }

  @Override
  public Mono<UpsertResult> upsert(VisitRecord record) {
    var item = mapper.toItem(record);
    var request =
        PutItemRequest.builder()
            .tableName(props.tableName())
            .item(item)
            // store only if new, or strictly newer than what's there (last-writer-wins)
            .conditionExpression("attribute_not_exists(#pk) OR #ua < :ua")
            .expressionAttributeNames(Map.of("#pk", VisitItem.PK, "#ua", VisitItem.UPDATED_AT))
            .expressionAttributeValues(
                Map.of(":ua", item.getOrDefault(VisitItem.UPDATED_AT, AttributeValue.fromS(""))))
            .returnValues(ReturnValue.ALL_OLD)
            .build();

    return Mono.fromFuture(client.putItem(request))
        .map(
            response -> {
              var outcome =
                  response.hasAttributes() && !response.attributes().isEmpty()
                      ? UpsertOutcome.UPDATED
                      : UpsertOutcome.CREATED;
              return new UpsertResult(record, outcome);
            })
        .onErrorResume(
            ConditionalCheckFailedException.class,
            e -> Mono.just(new UpsertResult(record, UpsertOutcome.DUPLICATE_IGNORED)));
  }

  @Override
  public Mono<VisitRecord> findById(String visitId) {
    var request =
        QueryRequest.builder()
            .tableName(props.tableName())
            .indexName("GSI2")
            .keyConditionExpression("#pk = :pk")
            .expressionAttributeNames(Map.of("#pk", VisitItem.GSI2_PK))
            .expressionAttributeValues(Map.of(":pk", AttributeValue.fromS("VISIT#" + visitId)))
            .limit(1)
            .build();

    return Mono.fromFuture(client.query(request))
        .flatMap(
            response ->
                response.items().isEmpty()
                    ? Mono.empty()
                    : Mono.just(mapper.fromItem(response.items().get(0))));
  }

  @Override
  public Flux<VisitRecord> findByWorker(String workerId) {
    var request =
        QueryRequest.builder()
            .tableName(props.tableName())
            .keyConditionExpression("#pk = :pk")
            .expressionAttributeNames(Map.of("#pk", VisitItem.PK))
            .expressionAttributeValues(Map.of(":pk", AttributeValue.fromS("WORKER#" + workerId)))
            .scanIndexForward(false) // newest first
            .build();

    return Mono.fromFuture(client.query(request))
        .flatMapMany(response -> Flux.fromIterable(response.items()))
        .map(mapper::fromItem);
  }
}
