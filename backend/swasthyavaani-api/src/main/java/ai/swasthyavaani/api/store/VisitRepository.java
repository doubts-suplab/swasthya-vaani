package ai.swasthyavaani.api.store;

import ai.swasthyavaani.domain.model.VisitRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Persistence seam for visit records. The Phase 3 PoC ships an in-memory implementation; the
 * DynamoDB single-table implementation ({@code data-model.md} §6) swaps in at Phase 5 without
 * changing callers — the idempotency contract lives here, not in the store.
 *
 * <p>Idempotency: {@link #upsert} is keyed on the client-generated {@code visitId}. Re-sending the
 * same or an older version is a no-op success; a strictly newer version (by {@code updatedAt})
 * wins.
 */
public interface VisitRepository {

  /** Idempotently store a record, keyed on {@code visitId} with last-writer-wins by updatedAt. */
  Mono<UpsertResult> upsert(VisitRecord record);

  /** Look up a single record by its {@code visitId}. */
  Mono<VisitRecord> findById(String visitId);

  /** All records for a worker, newest first (access pattern 1, {@code data-model.md} §6). */
  Flux<VisitRecord> findByWorker(String workerId);
}
