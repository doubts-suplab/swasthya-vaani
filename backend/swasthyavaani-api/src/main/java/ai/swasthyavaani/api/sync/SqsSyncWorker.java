package ai.swasthyavaani.api.sync;

import ai.swasthyavaani.api.store.AwsProperties;
import ai.swasthyavaani.api.store.UpsertResult;
import ai.swasthyavaani.api.store.VisitRepository;
import ai.swasthyavaani.domain.model.VisitRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Async reconcile path (T5-F03): drains the SQS sync queue and idempotently upserts each visit into
 * the {@link VisitRepository}. Because the upsert is keyed on {@code visitId} (last-writer-wins),
 * reprocessing a message is safe — a visit is never duplicated or lost even if delivery is
 * at-least-once. Active only under the {@code aws} profile.
 *
 * <p>This is the decoupled ingestion channel for producers that enqueue visit JSON; the interactive
 * {@code POST /visits/sync} remains the synchronous path. Compile-verified here — integration
 * against a live queue is the deploy step.
 */
@Component
@Profile("aws")
public class SqsSyncWorker {

  private static final Logger log = LoggerFactory.getLogger(SqsSyncWorker.class);

  private final SqsAsyncClient sqs;
  private final AwsProperties props;
  private final VisitRepository repository;
  private final ObjectMapper objectMapper;

  public SqsSyncWorker(
      SqsAsyncClient sqs,
      AwsProperties props,
      VisitRepository repository,
      ObjectMapper objectMapper) {
    this.sqs = sqs;
    this.props = props;
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${aws.sqs-poll-delay-ms:5000}")
  public void poll() {
    if (props.syncQueueUrl() == null || props.syncQueueUrl().isBlank()) {
      return;
    }
    var request =
        ReceiveMessageRequest.builder()
            .queueUrl(props.syncQueueUrl())
            .maxNumberOfMessages(10)
            .waitTimeSeconds(5)
            .build();
    Mono.fromFuture(sqs.receiveMessage(request))
        .flatMapMany(response -> Flux.fromIterable(response.messages()))
        .flatMap(this::process)
        .subscribe(null, e -> log.warn("SQS poll failed: {}", e.getClass().getSimpleName()));
  }

  private Mono<Void> process(Message message) {
    return ingest(message.body())
        .flatMap(result -> deleteMessage(message.receiptHandle()))
        .onErrorResume(
            e -> {
              log.warn("Dropping unprocessable SQS message ({})", e.getClass().getSimpleName());
              return Mono.empty();
            });
  }

  /** Parse a queued visit and idempotently upsert it. Package-visible for unit testing. */
  Mono<UpsertResult> ingest(String body) {
    return Mono.fromCallable(() -> objectMapper.readValue(body, VisitRecord.class))
        .flatMap(repository::upsert);
  }

  private Mono<Void> deleteMessage(String receiptHandle) {
    var request =
        DeleteMessageRequest.builder()
            .queueUrl(props.syncQueueUrl())
            .receiptHandle(receiptHandle)
            .build();
    return Mono.fromFuture(sqs.deleteMessage(request)).then();
  }
}
