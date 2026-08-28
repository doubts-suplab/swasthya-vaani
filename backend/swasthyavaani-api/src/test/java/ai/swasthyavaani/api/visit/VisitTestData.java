package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.enums.BeneficiaryCategory;
import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.enums.VisitType;
import ai.swasthyavaani.domain.model.Actions;
import ai.swasthyavaani.domain.model.Beneficiary;
import ai.swasthyavaani.domain.model.Observations;
import ai.swasthyavaani.domain.model.VisitRecord;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Shared synthetic visit record for Phase 2 tests (no real patient data — CLAUDE.md §6). */
final class VisitTestData {

  private VisitTestData() {}

  static VisitRecord draft() {
    var now = OffsetDateTime.parse("2026-08-28T09:14:00+05:30");
    return new VisitRecord(
        "8f2c1a44-6b0e-4c9a-9f1d-2e7a5b3c1d90",
        1,
        "ASHA-WB-1",
        "dev-1",
        VisitType.ANC,
        now,
        null,
        new Beneficiary(null, "Rekha Das", BeneficiaryCategory.PREGNANT_WOMAN, null, 24),
        new Observations(52.5, null, null, 28, List.of("fever since yesterday"), null),
        new Actions(List.of("IFA tablets"), null, LocalDate.parse("2026-09-11")),
        null,
        ConfirmationStatus.DRAFT,
        SyncStatus.PENDING,
        false,
        now,
        now,
        null);
  }
}
