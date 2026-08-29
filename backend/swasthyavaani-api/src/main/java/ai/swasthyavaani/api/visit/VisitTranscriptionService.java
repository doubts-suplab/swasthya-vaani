package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.api.artifact.ArtifactKeys;
import ai.swasthyavaani.api.artifact.ArtifactStore;
import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.SarvamException;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.SttMode;
import ai.swasthyavaani.sarvam.model.TranscriptionRequest;
import ai.swasthyavaani.sarvam.model.TranscriptionResult;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Phase 1 online happy path: audio → STT → extraction → schema validation → a {@code DRAFT} record
 * (assembled by {@link VisitAssembler}). Every visit comes back {@code DRAFT}/{@code PENDING}. When
 * an artifact store is wired ({@code aws} profile), the audio + transcript are uploaded to S3 and
 * referenced from the record's provenance ({@code data-model.md} §8).
 *
 * <p>Failures degrade rather than crash: if extraction returns non-conformant JSON the visit is
 * still returned as a {@code DRAFT} with the raw transcript surfaced for manual entry ({@code
 * data-model.md} §5). STT failure propagates as {@link SarvamException} for the error handler.
 */
@Service
public class VisitTranscriptionService {

  private static final Logger log = LoggerFactory.getLogger(VisitTranscriptionService.class);

  private final SarvamClient sarvam;
  private final SarvamProperties props;
  private final VisitAssembler assembler;
  private final ArtifactStore artifacts;
  private final Clock clock;

  public VisitTranscriptionService(
      SarvamClient sarvam,
      SarvamProperties props,
      VisitAssembler assembler,
      ArtifactStore artifacts,
      Clock clock) {
    this.sarvam = sarvam;
    this.props = props;
    this.assembler = assembler;
    this.artifacts = artifacts;
    this.clock = clock;
  }

  public Mono<VisitDraftResponse> transcribeAndExtract(TranscribeCommand cmd) {
    var visitId = cmd.visitId() != null ? cmd.visitId() : UUID.randomUUID().toString();
    var sttRequest =
        new TranscriptionRequest(
            cmd.audio(), cmd.filename(), cmd.contentType(), cmd.languageCode(), SttMode.CODEMIX);

    return sarvam
        .transcribe(sttRequest)
        .flatMap(
            transcription -> {
              var input = inputFor(cmd, transcription, visitId);
              return sarvam
                  .extractVisit(new ExtractionRequest(transcription.transcript(), contextFor(cmd)))
                  .map(extraction -> assembler.assemble(input, extraction))
                  .onErrorResume(
                      SarvamException.class,
                      ex -> {
                        log.warn(
                            "Extraction failed (HTTP {}); returning manual-entry draft",
                            ex.status());
                        return Mono.just(
                            assembler.manualEntry(
                                input,
                                "Automatic extraction failed; please enter the visit details from"
                                    + " the transcript."));
                      });
            });
  }

  private AssemblyInput inputFor(
      TranscribeCommand cmd, TranscriptionResult transcription, String visitId) {
    var sourceLanguage =
        transcription.languageCode() != null ? transcription.languageCode() : cmd.languageCode();

    String audioKey = null;
    String transcriptKey = null;
    if (artifacts.enabled()) {
      var now = OffsetDateTime.now(clock);
      audioKey = ArtifactKeys.audio(visitId, now, extension(cmd.contentType()));
      transcriptKey = ArtifactKeys.transcript(visitId, now);
      var contentType = cmd.contentType() != null ? cmd.contentType() : "application/octet-stream";
      artifacts.put(audioKey, cmd.audio(), contentType).subscribe();
      artifacts
          .put(
              transcriptKey,
              transcription.transcript().getBytes(StandardCharsets.UTF_8),
              "text/plain")
          .subscribe();
    }

    return new AssemblyInput(
        visitId,
        cmd.workerId(),
        cmd.deviceId(),
        cmd.visitType(),
        cmd.villageName(),
        cmd.state(),
        sourceLanguage,
        props.models().stt(),
        props.models().extraction(),
        transcription.transcript(),
        List.of(),
        audioKey,
        transcriptKey);
  }

  private static String extension(String contentType) {
    if (contentType == null) {
      return "bin";
    }
    if (contentType.contains("webm")) {
      return "webm";
    }
    if (contentType.contains("wav")) {
      return "wav";
    }
    if (contentType.contains("aac")) {
      return "aac";
    }
    if (contentType.contains("mp4") || contentType.contains("m4a")) {
      return "m4a";
    }
    if (contentType.contains("ogg")) {
      return "ogg";
    }
    return "bin";
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
}
