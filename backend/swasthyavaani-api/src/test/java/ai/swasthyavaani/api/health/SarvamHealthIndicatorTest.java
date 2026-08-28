package ai.swasthyavaani.api.health;

import ai.swasthyavaani.sarvam.config.SarvamProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import reactor.test.StepVerifier;

class SarvamHealthIndicatorTest {

  private static SarvamProperties propsWithKey(String key) {
    return new SarvamProperties(
        key,
        "https://api.sarvam.ai",
        "wss://api.sarvam.ai/speech-to-text/ws",
        new SarvamProperties.Languages("bn-IN", List.of("bn-IN", "en-IN")),
        new SarvamProperties.Models(
            "saaras:v3", "saaras:v3-realtime", "sarvam-m", "bulbul:v3", "sarvam-translate:v1"),
        new SarvamProperties.Tts("shubh", 22050),
        new SarvamProperties.Timeouts(Duration.ofSeconds(3), Duration.ofSeconds(30)),
        new SarvamProperties.Retry(3, Duration.ofMillis(500)));
  }

  @Test
  void upWhenKeyConfigured() {
    var indicator = new SarvamHealthIndicator(propsWithKey("sk-test"));
    StepVerifier.create(indicator.health())
        .assertNext(h -> org.junit.jupiter.api.Assertions.assertEquals(Status.UP, h.getStatus()))
        .verifyComplete();
  }

  @Test
  void degradedWhenKeyMissing() {
    var indicator = new SarvamHealthIndicator(propsWithKey(""));
    StepVerifier.create(indicator.health())
        .assertNext(
            h ->
                org.junit.jupiter.api.Assertions.assertEquals(
                    "NO_API_KEY", h.getStatus().getCode()))
        .verifyComplete();
  }
}
