package ai.swasthyavaani.api.sync;

import ai.swasthyavaani.api.store.VisitRepository;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.model.VisitRecord;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reconciles visit records from the client's offline queue into the backend registry. Each record
 * is stamped {@code SYNCED} and upserted idempotently on its {@code visitId} — so re-draining a
 * queue after a flaky connection never duplicates or loses a visit ({@code data-model.md} §7). This
 * is the server half of the offline-first guarantee; Phase 5 puts SQS + DynamoDB behind the same
 * {@link VisitRepository} seam.
 */
@Service
public class VisitSyncService {

  private final VisitRepository repository;
  private final Clock clock;

  public VisitSyncService(VisitRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Sync one record. Idempotent: safe to call repeatedly with the same {@code visitId}. */
  public Mono<SyncResult> sync(VisitRecord record) {
    var synced = markSynced(record);
    return repository
        .upsert(synced)
        .map(
            result ->
                new SyncResult(
                    result.stored().visitId(), result.outcome(), result.stored().syncStatus()));
  }

  /** Drain a batch (the queue) in order; each entry is independently idempotent. */
  public Flux<SyncResult> syncAll(List<VisitRecord> records) {
    return Flux.fromIterable(records).concatMap(this::sync);
  }

  private VisitRecord markSynced(VisitRecord r) {
    return new VisitRecord(
        r.visitId(),
        r.schemaVersion(),
        r.workerId(),
        r.deviceId(),
        r.visitType(),
        r.visitTimestamp(),
        r.location(),
        r.beneficiary(),
        r.observations(),
        r.actions(),
        r.provenance(),
        r.confirmationStatus(),
        SyncStatus.SYNCED,
        r.createdOffline(),
        r.createdAt(),
        r.updatedAt(),
        OffsetDateTime.now(clock));
  }
}
