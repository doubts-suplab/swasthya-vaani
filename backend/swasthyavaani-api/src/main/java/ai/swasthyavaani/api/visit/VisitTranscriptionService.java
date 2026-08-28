package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.api.validation.ValidationResult;
import ai.swasthyavaani.api.validation.VisitExtractionValidator;
import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.enums.VisitType;
import ai.swasthyavaani.domain.extraction.VisitExtraction;
import ai.swasthyavaani.domain.model.Location;
import ai.swasthyavaani.domain.model.Provenance;
import ai.swasthyavaani.domain.model.VisitRecord;
import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.SarvamException;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.SttMode;
import ai.swasthyavaani.sarvam.model.TranscriptionRequest;
import ai.swasthyavaani.sarvam.model.TranscriptionResult;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Phase 1 online happy path: audio → STT → extraction → schema validation → a {@code DRAFT} {@link
 * VisitRecord}. Every visit comes back {@code DRAFT}/{@code PENDING}; the confirm/edit loop (Phase
 * 2) and offline sync (Phase 3) build on this.
 *
 * <p>Failures degrade rather than crash: if extraction returns non-conformant JSON the visit is
 * still returned as a {@code DRAFT} with the raw transcript surfaced for manual entry ({@code
 * data-model.md} §5). STT failure propagates as {@link SarvamException} for the error handler to
 * translate.
 */
@Service
public class VisitTranscriptionService {

  private static final Logger log = LoggerFactory.getLogger(VisitTranscriptionService.class);
  private static final String UNKNOWN = "UNKNOWN";

  private final SarvamClient sarvam;
  private final SarvamProperties props;
  private final VisitExtractionValidator validator;
  private final Clock clock;

  public VisitTranscriptionService(
      SarvamClient sarvam,
      SarvamProperties props,
      VisitExtractionValidator validator,
      Clock clock) {
    this.sarvam = sarvam;
    this.props = props;
    this.validator = validator;
    this.clock = clock;
  }

  public Mono<VisitDraftResponse> transcribeAndExtract(TranscribeCommand cmd) {
    var sttRequest =
        new TranscriptionRequest(
            cmd.audio(), cmd.filename(), cmd.contentType(), cmd.languageCode(), SttMode.CODEMIX);

    return sarvam
        .transcribe(sttRequest)
        .flatMap(
            transcription ->
                sarvam
                    .extractVisit(
                        new ExtractionRequest(transcription.transcript(), contextFor(cmd)))
                    .map(extraction -> assemble(cmd, transcription, extraction))
                    .onErrorResume(
                        SarvamException.class,
                        ex -> {
                          log.warn(
                              "Extraction failed (HTTP {}); returning manual-entry draft",
                              ex.status());
                          return Mono.just(assembleManualEntry(cmd, transcription));
                        }));
  }

  // --- assembly ------------------------------------------------------------

  private VisitDraftResponse assemble(
      TranscribeCommand cmd, TranscriptionResult transcription, VisitExtraction extraction) {
    ValidationResult validation = validator.validate(extraction);

    List<String> warnings = new ArrayList<>();
    if (extraction.warnings() != null) {
      warnings.addAll(extraction.warnings());
    }
    warnings.addAll(validation.messages());

    var record =
        baseRecord(cmd, transcription, warnings)
            .withExtraction(
                extraction.beneficiary(), extraction.observations(), extraction.actions());

    return new VisitDraftResponse(
        record, transcription.transcript(), validation.valid(), validation.messages());
  }

  /**
   * Extraction produced nothing usable — return a shell draft with the transcript for manual entry.
   */
  private VisitDraftResponse assembleManualEntry(
      TranscribeCommand cmd, TranscriptionResult transcription) {
    List<String> messages =
        List.of("Automatic extraction failed; please enter the visit details from the transcript.");
    var record = baseRecord(cmd, transcription, messages).build();
    return new VisitDraftResponse(record, transcription.transcript(), false, messages);
  }

  private RecordBuilder baseRecord(
      TranscribeCommand cmd, TranscriptionResult transcription, List<String> warnings) {
    var now = OffsetDateTime.now(clock);
    var provenance =
        new Provenance(
            transcription.languageCode() != null
                ? transcription.languageCode()
                : cmd.languageCode(),
            props.models().stt(),
            props.models().extraction(),
            null,
            warnings.isEmpty() ? null : List.copyOf(warnings),
            null,
            null);

    return new RecordBuilder(
        cmd.visitId() != null ? cmd.visitId() : UUID.randomUUID().toString(),
        cmd.workerId() != null ? cmd.workerId() : UNKNOWN,
        cmd.deviceId() != null ? cmd.deviceId() : UNKNOWN,
        cmd.visitType() != null ? cmd.visitType() : VisitType.GENERAL,
        locationFor(cmd),
        provenance,
        now);
  }

  private static Location locationFor(TranscribeCommand cmd) {
    if (cmd.villageName() == null && cmd.state() == null) {
      return null;
    }
    return new Location(cmd.villageName(), null, null, cmd.state());
  }

  private String contextFor(TranscribeCommand cmd) {
    var sb = new StringBuilder("Expected languages: Bengali + English, code-mixed.");
    if (cmd.villageName() != null || cmd.state() != null) {
      sb.append(" Worker's default location: ")
          .append(cmd.villageName() != null ? cmd.villageName() : "")
          .append(cmd.state() != null ? ", " + cmd.state() : "")
          .append('.');
    }
    return sb.toString();
  }

  /**
   * Small builder so the two assembly paths share record construction without duplicating fields.
   */
  private record RecordBuilder(
      String visitId,
      String workerId,
      String deviceId,
      VisitType visitType,
      Location location,
      Provenance provenance,
      OffsetDateTime now) {

    VisitRecord build() {
      return withExtraction(null, null, null);
    }

    VisitRecord withExtraction(
        ai.swasthyavaani.domain.model.Beneficiary beneficiary,
        ai.swasthyavaani.domain.model.Observations observations,
        ai.swasthyavaani.domain.model.Actions actions) {
      return new VisitRecord(
          visitId,
          0, // defaults to current schema version
          workerId,
          deviceId,
          visitType,
          now,
          location,
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
  }
}
