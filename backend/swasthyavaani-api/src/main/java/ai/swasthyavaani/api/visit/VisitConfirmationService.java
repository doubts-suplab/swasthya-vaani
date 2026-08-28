package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.model.VisitRecord;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

/**
 * Applies the confirm/edit state machine to a visit record ({@code data-model.md} §4). The
 * transition is enforced server-side; the record is returned with the new confirmation status and a
 * bumped {@code updatedAt}. Persistence is Phase 3 — this stage is a pure, stateless transition.
 *
 * <p>Any active record can move to {@code CONFIRMED} (approve) or {@code EDITED}
 * (approve-with-changes). A record already {@code SUPERSEDED} by a newer version (sync status)
 * cannot be re-confirmed — confirming a stale copy is rejected.
 */
@Service
public class VisitConfirmationService {

  private final Clock clock;

  public VisitConfirmationService(Clock clock) {
    this.clock = clock;
  }

  public VisitRecord confirm(VisitRecord record, boolean edited) {
    if (record.syncStatus() == SyncStatus.SUPERSEDED) {
      throw new InvalidTransitionException("A superseded visit cannot be confirmed.");
    }
    var target = edited ? ConfirmationStatus.EDITED : ConfirmationStatus.CONFIRMED;
    var now = OffsetDateTime.now(clock);

    return new VisitRecord(
        record.visitId(),
        record.schemaVersion(),
        record.workerId(),
        record.deviceId(),
        record.visitType(),
        record.visitTimestamp(),
        record.location(),
        record.beneficiary(),
        record.observations(),
        record.actions(),
        record.provenance(),
        target,
        record.syncStatus(),
        record.createdOffline(),
        record.createdAt(),
        now,
        record.syncedAt());
  }
}
