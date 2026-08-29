package ai.swasthyavaani.api.realtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.swasthyavaani.sarvam.config.SarvamProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealtimeUriTest {

  private static SarvamProperties props() {
    return new SarvamProperties(
        "SECRET-KEY",
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
  void buildsRealtimeUriWithModelAndLanguage() {
    var uri = RealtimeUri.build(props(), "bn-IN", 16000).toString();
    assertTrue(uri.startsWith("wss://api.sarvam.ai/speech-to-text/ws"), uri);
    assertTrue(
        uri.contains("model=saaras:v3-realtime") || uri.contains("model=saaras%3Av3-realtime"),
        uri);
    assertTrue(uri.contains("sample_rate=16000"), uri);
    assertTrue(uri.contains("language-code=bn-IN"), uri);
  }

  @Test
  void neverPutsTheApiKeyInTheUri() {
    var uri = RealtimeUri.build(props(), "bn-IN", 16000).toString();
    assertFalse(uri.contains("SECRET-KEY"), "API key must travel in a header, not the URL");
  }
}
