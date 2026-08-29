package ai.swasthyavaani.api.store;

import ai.swasthyavaani.domain.model.VisitRecord;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * In-memory {@link VisitRepository} — the default store, used everywhere except the {@code aws}
 * profile (where {@code DynamoDbVisitRepository} takes over). Thread-safe and idempotent; it models
 * the exact upsert semantics the DynamoDB implementation must honour (conditional write / dedup via
 * GSI2, {@code data-model.md} §6, §7), so the sync logic and its tests are correct without AWS.
 */
@Repository
@Profile("!aws")
public class InMemoryVisitRepository implements VisitRepository {

  private final ConcurrentHashMap<String, VisitRecord> store = new ConcurrentHashMap<>();

  @Override
  public Mono<UpsertResult> upsert(VisitRecord record) {
    return Mono.fromCallable(
        () -> {
          var outcome = new AtomicReference<UpsertOutcome>();
          var stored =
              store.compute(
                  record.visitId(),
                  (id, existing) -> {
                    if (existing == null) {
                      outcome.set(UpsertOutcome.CREATED);
                      return record;
                    }
                    if (isNewer(record, existing)) {
                      outcome.set(UpsertOutcome.UPDATED);
                      return record;
                    }
                    outcome.set(UpsertOutcome.DUPLICATE_IGNORED);
                    return existing;
                  });
          return new UpsertResult(stored, outcome.get());
        });
  }

  @Override
  public Mono<VisitRecord> findById(String visitId) {
    return Mono.justOrEmpty(store.get(visitId));
  }

  @Override
  public Flux<VisitRecord> findByWorker(String workerId) {
    return Flux.fromStream(
        store.values().stream()
            .filter(r -> workerId.equals(r.workerId()))
            .sorted(Comparator.comparing(VisitRecord::visitTimestamp).reversed()));
  }

  private static boolean isNewer(VisitRecord incoming, VisitRecord existing) {
    OffsetDateTime a = incoming.updatedAt();
    OffsetDateTime b = existing.updatedAt();
    return a != null && (b == null || a.isAfter(b));
  }
}
