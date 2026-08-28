package ai.swasthyavaani.sarvam.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * All Sarvam configuration, bound from the {@code sarvam.*} namespace. Model ids and languages are
 * config-driven so a model swap or a new state is a config change, not a code change ({@code
 * CLAUDE.md} §6; {@code architecture.md} ADR-003).
 *
 * <p>The API key comes from the environment ({@code SARVAM_API_KEY}) and is never committed.
 */
@ConfigurationProperties(prefix = "sarvam")
public record SarvamProperties(
    String apiKey,
    @DefaultValue("https://api.sarvam.ai") String baseUrl,
    @DefaultValue("wss://api.sarvam.ai/speech-to-text/ws") String realtimeUrl,
    @DefaultValue Languages languages,
    @DefaultValue Models models,
    @DefaultValue Tts tts,
    @DefaultValue Timeouts timeout,
    @DefaultValue Retry retry) {

  /** Target languages. Default is code-mixed Bengali + English (Indian). */
  public record Languages(
      @DefaultValue("bn-IN") String defaultTarget,
      @DefaultValue({"bn-IN", "en-IN"}) List<String> supported) {}

  /** Concrete Sarvam model ids per capability (verified Aug 2026; see sarvam-integration.md §2). */
  public record Models(
      @DefaultValue("saaras:v3") String stt,
      @DefaultValue("saaras:v3-realtime") String sttRealtime,
      @DefaultValue("sarvam-m") String extraction,
      @DefaultValue("bulbul:v3") String tts,
      @DefaultValue("sarvam-translate:v1") String translate) {}

  /** Text-to-speech defaults. */
  public record Tts(
      @DefaultValue("shubh") String defaultSpeaker, @DefaultValue("22050") int sampleRate) {}

  /** Per-call timeouts. */
  public record Timeouts(
      @DefaultValue("3s") Duration connect, @DefaultValue("30s") Duration read) {}

  /** Bounded retry-with-backoff policy for retryable failures (429/503/network). */
  public record Retry(
      @DefaultValue("3") int maxAttempts, @DefaultValue("500ms") Duration initialBackoff) {}
}
