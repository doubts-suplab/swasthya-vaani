package ai.swasthyavaani.api.ocr;

import ai.swasthyavaani.api.artifact.ArtifactKeys;
import ai.swasthyavaani.api.artifact.ArtifactStore;
import ai.swasthyavaani.api.visit.AssemblyInput;
import ai.swasthyavaani.api.visit.VisitAssembler;
import ai.swasthyavaani.api.visit.VisitDraftResponse;
import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.SarvamException;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.OcrRequest;
import ai.swasthyavaani.sarvam.model.OcrResult;
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
 * Phase 4 OCR ingest: photograph → Sarvam Vision digitise → extraction → schema validation → a
 * {@code DRAFT} record. Reuses the same extraction + {@link VisitAssembler} as the spoken path, so
 * a paper register lands in the identical schema and review loop ({@code roadmap.md} T3-F04). When
 * an artifact store is wired ({@code aws} profile) the digitised text is uploaded to S3 (§8).
 *
 * <p>OCR failure surfaces as {@link SarvamException} (handled as 502); a failed extraction degrades
 * to a manual-entry draft with the digitised text surfaced.
 */
@Service
public class VisitOcrService {

  private static final Logger log = LoggerFactory.getLogger(VisitOcrService.class);
  private static final String OCR_NOTE =
      "Ingested from a photographed record via OCR (Sarvam Vision).";

  private final SarvamClient sarvam;
  private final SarvamProperties props;
  private final VisitAssembler assembler;
  private final ArtifactStore artifacts;
  private final Clock clock;

  public VisitOcrService(
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

  public Mono<VisitDraftResponse> ingestPhoto(OcrCommand cmd) {
    var visitId = cmd.visitId() != null ? cmd.visitId() : UUID.randomUUID().toString();
    var ocrRequest =
        new OcrRequest(cmd.image(), cmd.filename(), cmd.contentType(), cmd.languageCode());

    return sarvam
        .ocr(ocrRequest)
        .flatMap(
            ocr -> {
              var input = inputFor(cmd, ocr, visitId);
              return sarvam
                  .extractVisit(new ExtractionRequest(ocr.text(), context()))
                  .map(extraction -> assembler.assemble(input, extraction))
                  .onErrorResume(
                      SarvamException.class,
                      ex -> {
                        log.warn(
                            "Extraction from OCR failed (HTTP {}); manual-entry draft",
                            ex.status());
                        return Mono.just(
                            assembler.manualEntry(
                                input,
                                "Automatic extraction from the photo failed; please enter the"
                                    + " details from the digitised text."));
                      });
            });
  }

  private AssemblyInput inputFor(OcrCommand cmd, OcrResult ocr, String visitId) {
    String ocrKey = null;
    if (artifacts.enabled()) {
      var now = OffsetDateTime.now(clock);
      ocrKey = ArtifactKeys.ocr(visitId, now, 1);
      artifacts
          .put(ocrKey, ocr.text().getBytes(StandardCharsets.UTF_8), "application/json")
          .subscribe();
    }

    return new AssemblyInput(
        visitId,
        cmd.workerId(),
        cmd.deviceId(),
        cmd.visitType(),
        cmd.villageName(),
        cmd.state(),
        cmd.languageCode(),
        null, // no STT model — this record came from OCR
        props.models().extraction(),
        ocr.text(),
        List.of(OCR_NOTE),
        null,
        ocrKey);
  }

  private static String context() {
    return "This text was OCR'd from a handwritten/printed Indian health register or MCP card."
        + " Expected languages: Bengali + English, code-mixed.";
  }
}
