package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.enums.VisitType;
import ai.swasthyavaani.domain.model.VisitRecord;
import java.util.Locale;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Visit endpoints. Phase 1 exposes the online happy path: upload audio, get back a {@code DRAFT}
 * visit record. All AI work is delegated to {@link VisitTranscriptionService} (no business logic in
 * the controller, {@code CLAUDE.md} §5).
 */
@RestController
@RequestMapping("/api/v1/visits")
public class VisitController {

  private final VisitTranscriptionService service;
  private final VisitReadbackService readbackService;
  private final VisitConfirmationService confirmationService;

  public VisitController(
      VisitTranscriptionService service,
      VisitReadbackService readbackService,
      VisitConfirmationService confirmationService) {
    this.service = service;
    this.readbackService = readbackService;
    this.confirmationService = confirmationService;
  }

  /**
   * Transcribe a visit recording and return the extracted draft record.
   *
   * @param file the audio recording (required)
   */
  @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<VisitDraftResponse> transcribe(
      @RequestPart("file") FilePart file,
      @RequestPart(value = "workerId", required = false) String workerId,
      @RequestPart(value = "deviceId", required = false) String deviceId,
      @RequestPart(value = "languageCode", required = false) String languageCode,
      @RequestPart(value = "visitType", required = false) String visitType,
      @RequestPart(value = "villageName", required = false) String villageName,
      @RequestPart(value = "state", required = false) String state,
      @RequestPart(value = "visitId", required = false) String visitId) {

    return toBytes(file)
        .map(
            bytes ->
                new TranscribeCommand(
                    bytes,
                    file.filename(),
                    contentType(file),
                    languageCode,
                    workerId,
                    deviceId,
                    parseVisitType(visitType),
                    villageName,
                    state,
                    visitId))
        .flatMap(service::transcribeAndExtract);
  }

  /**
   * Speak a draft record back in the worker's language for confirmation (Phase 2). Returns the
   * spoken text and base64 audio.
   *
   * @param visit the record to read back
   * @param targetLanguage BCP-47 language, or omit for the configured default
   */
  @PostMapping(value = "/readback", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ReadbackResponse> readback(
      @RequestBody VisitRecord visit,
      @RequestParam(value = "targetLanguage", required = false) String targetLanguage) {
    return readbackService.readback(visit, targetLanguage);
  }

  /**
   * Confirm a (possibly edited) record. Applies the {@code DRAFT → CONFIRMED|EDITED} transition and
   * returns the updated record.
   */
  @PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<VisitRecord> confirm(@RequestBody ConfirmRequest request) {
    return Mono.fromSupplier(() -> confirmationService.confirm(request.visit(), request.edited()));
  }

  private static Mono<byte[]> toBytes(FilePart file) {
    return DataBufferUtils.join(file.content())
        .map(
            buffer -> {
              byte[] bytes = new byte[buffer.readableByteCount()];
              buffer.read(bytes);
              DataBufferUtils.release(buffer);
              return bytes;
            });
  }

  private static String contentType(FilePart file) {
    MediaType type = file.headers().getContentType();
    return type != null ? type.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
  }

  private static VisitType parseVisitType(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return VisitType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null; // unknown -> service defaults to GENERAL
    }
  }
}
