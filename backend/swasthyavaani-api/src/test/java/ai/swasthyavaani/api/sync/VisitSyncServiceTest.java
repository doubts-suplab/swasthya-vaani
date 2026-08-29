package ai.swasthyavaani.api.sync;

import ai.swasthyavaani.api.store.InMemoryVisitRepository;
import ai.swasthyavaani.api.store.UpsertOutcome;
import ai.swasthyavaani.api.visit.VisitTestData;
import ai.swasthyavaani.domain.enums.SyncStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class VisitSyncServiceTest {

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-08-28T12:00:00Z"), ZoneId.of("Asia/Kolkata"));
  private final InMemoryVisitRepository repo = new InMemoryVisitRepository();
  private final VisitSyncService service =
      new VisitSyncService(
          repo, clock, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

  @Test
  void syncMarksRecordSyncedAndStamps() {
    var record = VisitTestData.draftWith("v-1", OffsetDateTime.parse("2026-08-28T09:14:00+05:30"));

    StepVerifier.create(service.sync(record))
        .assertNext(
            result -> {
              Assertions.assertEquals(UpsertOutcome.CREATED, result.outcome());
              Assertions.assertEquals(SyncStatus.SYNCED, result.syncStatus());
            })
        .verifyComplete();

    StepVerifier.create(repo.findById("v-1"))
        .assertNext(
            stored -> {
              Assertions.assertEquals(SyncStatus.SYNCED, stored.syncStatus());
              Assertions.assertNotNull(stored.syncedAt());
            })
        .verifyComplete();
  }

  @Test
  void drainingTheSameQueueTwiceNeverDuplicates() {
    var t = OffsetDateTime.parse("2026-08-28T09:14:00+05:30");
    var queue = List.of(VisitTestData.draftWith("a", t), VisitTestData.draftWith("b", t));

    // First drain: both created.
    StepVerifier.create(service.syncAll(queue).collectList())
        .assertNext(results -> Assertions.assertEquals(2, results.size()))
        .verifyComplete();

    // Second drain (flaky-connection retry): both ignored, nothing duplicated.
    StepVerifier.create(service.syncAll(queue))
        .expectNextMatches(r -> r.outcome() == UpsertOutcome.DUPLICATE_IGNORED)
        .expectNextMatches(r -> r.outcome() == UpsertOutcome.DUPLICATE_IGNORED)
        .verifyComplete();

    StepVerifier.create(repo.findByWorker("ASHA-WB-1").count())
        .assertNext(count -> Assertions.assertEquals(2L, count))
        .verifyComplete();
  }
}
