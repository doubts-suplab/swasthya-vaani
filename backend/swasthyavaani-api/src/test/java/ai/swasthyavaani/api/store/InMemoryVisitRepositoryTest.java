package ai.swasthyavaani.api.store;

import ai.swasthyavaani.api.visit.VisitTestData;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class InMemoryVisitRepositoryTest {

  private final InMemoryVisitRepository repo = new InMemoryVisitRepository();

  @Test
  void firstUpsertCreates() {
    var record = VisitTestData.draftWith("v-1", OffsetDateTime.parse("2026-08-28T09:14:00+05:30"));
    StepVerifier.create(repo.upsert(record))
        .assertNext(
            r -> org.junit.jupiter.api.Assertions.assertEquals(UpsertOutcome.CREATED, r.outcome()))
        .verifyComplete();
  }

  @Test
  void resendingSameOrOlderIsIdempotentNoOp() {
    var t1 = OffsetDateTime.parse("2026-08-28T09:14:00+05:30");
    var first = VisitTestData.draftWith("v-2", t1);
    repo.upsert(first).block();

    // Same version again -> ignored, no duplicate.
    StepVerifier.create(repo.upsert(VisitTestData.draftWith("v-2", t1)))
        .assertNext(
            r ->
                org.junit.jupiter.api.Assertions.assertEquals(
                    UpsertOutcome.DUPLICATE_IGNORED, r.outcome()))
        .verifyComplete();

    // Older version -> also ignored.
    StepVerifier.create(repo.upsert(VisitTestData.draftWith("v-2", t1.minusMinutes(5))))
        .assertNext(
            r ->
                org.junit.jupiter.api.Assertions.assertEquals(
                    UpsertOutcome.DUPLICATE_IGNORED, r.outcome()))
        .verifyComplete();
  }

  @Test
  void newerVersionWins() {
    var t1 = OffsetDateTime.parse("2026-08-28T09:14:00+05:30");
    repo.upsert(VisitTestData.draftWith("v-3", t1)).block();

    var newer = VisitTestData.draftWith("v-3", t1.plusMinutes(3));
    StepVerifier.create(repo.upsert(newer))
        .assertNext(
            r -> org.junit.jupiter.api.Assertions.assertEquals(UpsertOutcome.UPDATED, r.outcome()))
        .verifyComplete();

    StepVerifier.create(repo.findById("v-3"))
        .assertNext(
            r -> org.junit.jupiter.api.Assertions.assertEquals(t1.plusMinutes(3), r.updatedAt()))
        .verifyComplete();
  }
}
