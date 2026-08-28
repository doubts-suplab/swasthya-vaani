package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.enums.VisitType;
import java.util.Locale;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

  public VisitController(VisitTranscriptionService service) {
    this.service = service;
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
