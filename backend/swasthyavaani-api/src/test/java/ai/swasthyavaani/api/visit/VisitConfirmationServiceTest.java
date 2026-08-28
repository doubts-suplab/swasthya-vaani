package ai.swasthyavaani.api.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.model.VisitRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class VisitConfirmationServiceTest {

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-08-28T12:00:00Z"), ZoneId.of("Asia/Kolkata"));
  private final VisitConfirmationService service = new VisitConfirmationService(clock);

  @Test
  void confirmingWithoutEditsMarksConfirmedAndBumpsUpdatedAt() {
    var draft = VisitTestData.draft();
    VisitRecord result = service.confirm(draft, false);

    assertEquals(ConfirmationStatus.CONFIRMED, result.confirmationStatus());
    assertTrue(result.updatedAt().isAfter(draft.updatedAt()));
    assertEquals(draft.visitId(), result.visitId());
    assertEquals(draft.createdAt(), result.createdAt());
  }

  @Test
  void confirmingWithEditsMarksEdited() {
    var result = service.confirm(VisitTestData.draft(), true);
    assertEquals(ConfirmationStatus.EDITED, result.confirmationStatus());
  }

  @Test
  void supersededRecordCannotBeConfirmed() {
    var draft = VisitTestData.draft();
    var superseded =
        new VisitRecord(
            draft.visitId(),
            draft.schemaVersion(),
            draft.workerId(),
            draft.deviceId(),
            draft.visitType(),
            draft.visitTimestamp(),
            draft.location(),
            draft.beneficiary(),
            draft.observations(),
            draft.actions(),
            draft.provenance(),
            ConfirmationStatus.DRAFT,
            SyncStatus.SUPERSEDED,
            false,
            draft.createdAt(),
            draft.updatedAt(),
            null);

    assertThrows(InvalidTransitionException.class, () -> service.confirm(superseded, false));
  }
}
