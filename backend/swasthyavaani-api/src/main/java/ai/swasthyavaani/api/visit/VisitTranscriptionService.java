package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.SarvamException;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.SttMode;
import ai.swasthyavaani.sarvam.model.TranscriptionRequest;
import ai.swasthyavaani.sarvam.model.TranscriptionResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Phase 1 online happy path: audio → STT → extraction → schema validation → a {@code DRAFT} record
 * (assembled by {@link VisitAssembler}). Every visit comes back {@code DRAFT}/{@code PENDING}.
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

  public VisitTranscriptionService(
      SarvamClient sarvam, SarvamProperties props, VisitAssembler assembler) {
    this.sarvam = sarvam;
    this.props = props;
    this.assembler = assembler;
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
                    .map(extraction -> assembler.assemble(inputFor(cmd, transcription), extraction))
                    .onErrorResume(
                        SarvamException.class,
                        ex -> {
                          log.warn(
                              "Extraction failed (HTTP {}); returning manual-entry draft",
                              ex.status());
                          return Mono.just(
                              assembler.manualEntry(
                                  inputFor(cmd, transcription),
                                  "Automatic extraction failed; please enter the visit details from"
                                      + " the transcript."));
                        }));
  }

  private AssemblyInput inputFor(TranscribeCommand cmd, TranscriptionResult transcription) {
    var sourceLanguage =
        transcription.languageCode() != null ? transcription.languageCode() : cmd.languageCode();
    return new AssemblyInput(
        cmd.visitId(),
        cmd.workerId(),
        cmd.deviceId(),
        cmd.visitType(),
        cmd.villageName(),
        cmd.state(),
        sourceLanguage,
        props.models().stt(),
        props.models().extraction(),
        transcription.transcript(),
        List.of());
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
