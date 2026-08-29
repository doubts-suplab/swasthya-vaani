package ai.swasthyavaani.api.artifact;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Deterministic S3 keys for a visit's artifacts ({@code data-model.md} §8), keyed by {@code
 * visitId}.
 */
public final class ArtifactKeys {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private ArtifactKeys() {}

  public static String audio(String visitId, OffsetDateTime when, String extension) {
    return "audio/" + DATE.format(when) + "/" + visitId + "." + extension;
  }

  public static String transcript(String visitId, OffsetDateTime when) {
    return "transcripts/" + DATE.format(when) + "/" + visitId + ".txt";
  }

  public static String ocr(String visitId, OffsetDateTime when, int page) {
    return "ocr/" + DATE.format(when) + "/" + visitId + "-page" + page + ".json";
  }
}
