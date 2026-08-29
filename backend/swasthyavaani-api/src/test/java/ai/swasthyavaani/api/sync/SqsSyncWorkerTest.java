package ai.swasthyavaani.api.sync;

import static org.mockito.Mockito.mock;

import ai.swasthyavaani.api.store.AwsProperties;
import ai.swasthyavaani.api.store.InMemoryVisitRepository;
import ai.swasthyavaani.api.store.UpsertOutcome;
import ai.swasthyavaani.api.visit.VisitTestData;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

class SqsSyncWorkerTest {

  private final ObjectMapper mapper =
      JsonMapper.builder()
          .findAndAddModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .build();
  private final InMemoryVisitRepository repo = new InMemoryVisitRepository();
  private final SqsSyncWorker worker =
      new SqsSyncWorker(
          mock(SqsAsyncClient.class),
          new AwsProperties("ap-south-1", "swasthyavaani", "bucket", "queue-url", null),
          repo,
          mapper);

  @Test
  void ingestParsesAqueuedVisitAndUpsertsIdempotently() throws Exception {
    var record = VisitTestData.draftWith("v-1", OffsetDateTime.parse("2026-08-28T09:14:00+05:30"));
    var body = mapper.writeValueAsString(record);

    StepVerifier.create(worker.ingest(body))
        .assertNext(r -> Assertions.assertEquals(UpsertOutcome.CREATED, r.outcome()))
        .verifyComplete();

    // Re-ingesting the same message is a no-op (at-least-once delivery is safe).
    StepVerifier.create(worker.ingest(body))
        .assertNext(r -> Assertions.assertEquals(UpsertOutcome.DUPLICATE_IGNORED, r.outcome()))
        .verifyComplete();
  }
}
