package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.api.validation.ValidationResult;
import ai.swasthyavaani.api.validation.VisitExtractionValidator;
import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.enums.VisitType;
import ai.swasthyavaani.domain.extraction.VisitExtraction;
import ai.swasthyavaani.domain.model.Actions;
import ai.swasthyavaani.domain.model.Beneficiary;
import ai.swasthyavaani.domain.model.Location;
import ai.swasthyavaani.domain.model.Observations;
import ai.swasthyavaani.domain.model.Provenance;
import ai.swasthyavaani.domain.model.VisitRecord;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Builds a {@code DRAFT} {@link VisitRecord} from an extraction (or a manual-entry shell when
 * extraction failed), validating against the schema and merging warnings. Shared by the spoken
 * (Phase 1) and OCR (Phase 4) ingest paths so record construction lives in one place.
 */
@Component
public class VisitAssembler {

  private static final String UNKNOWN = "UNKNOWN";

  private final VisitExtractionValidator validator;
  private final Clock clock;

  public VisitAssembler(VisitExtractionValidator validator, Clock clock) {
    this.validator = validator;
    this.clock = clock;
  }

  /** Assemble a validated draft from an extraction. */
  public VisitDraftResponse assemble(AssemblyInput in, VisitExtraction extraction) {
    ValidationResult validation = validator.validate(extraction);

    List<String> warnings = new ArrayList<>(orEmpty(in.extraWarnings()));
    if (extraction.warnings() != null) {
      warnings.addAll(extraction.warnings());
    }
    warnings.addAll(validation.messages());

    var record =
        record(
            in,
            warnings,
            extraction.beneficiary(),
            extraction.observations(),
            extraction.actions());
    return new VisitDraftResponse(
        record, in.sourceText(), validation.valid(), validation.messages());
  }

  /** Assemble a shell draft when extraction produced nothing usable — surface the source text. */
  public VisitDraftResponse manualEntry(AssemblyInput in, String reason) {
    List<String> messages = List.of(reason);
    List<String> warnings = new ArrayList<>(orEmpty(in.extraWarnings()));
    warnings.addAll(messages);
    var record = record(in, warnings, null, null, null);
    return new VisitDraftResponse(record, in.sourceText(), false, messages);
  }

  private VisitRecord record(
      AssemblyInput in,
      List<String> warnings,
      Beneficiary beneficiary,
      Observations observations,
      Actions actions) {
    var now = OffsetDateTime.now(clock);
    var provenance =
        new Provenance(
            in.sourceLanguage(),
            in.sttModel(),
            in.extractionModel(),
            null,
            warnings.isEmpty() ? null : List.copyOf(warnings),
            null,
            null);

    return new VisitRecord(
        in.visitId() != null ? in.visitId() : UUID.randomUUID().toString(),
        0, // defaults to current schema version
        in.workerId() != null ? in.workerId() : UNKNOWN,
        in.deviceId() != null ? in.deviceId() : UNKNOWN,
        in.visitType() != null ? in.visitType() : VisitType.GENERAL,
        now,
        location(in),
        beneficiary,
        observations,
        actions,
        provenance,
        ConfirmationStatus.DRAFT,
        SyncStatus.PENDING,
        false,
        now,
        now,
        null);
  }

  private static Location location(AssemblyInput in) {
    if (in.villageName() == null && in.state() == null) {
      return null;
    }
    return new Location(in.villageName(), null, null, in.state());
  }

  private static <T> List<T> orEmpty(List<T> list) {
    return list != null ? list : List.of();
  }
}
