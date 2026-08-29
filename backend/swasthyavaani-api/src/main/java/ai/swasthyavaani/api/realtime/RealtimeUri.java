package ai.swasthyavaani.api.realtime;

import ai.swasthyavaani.sarvam.config.SarvamProperties;
import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the upstream Sarvam realtime STT WebSocket URI ({@code saaras:v3-realtime}). The API key
 * is deliberately NOT a query parameter — it travels in the {@code Api-Subscription-Key} header
 * from the server, so it never reaches the browser ({@code sarvam-integration.md} §4).
 */
public final class RealtimeUri {

  private RealtimeUri() {}

  public static URI build(SarvamProperties props, String languageCode, int sampleRate) {
    var builder =
        UriComponentsBuilder.fromUriString(props.realtimeUrl())
            .queryParam("model", props.models().sttRealtime())
            .queryParam("sample_rate", sampleRate);
    if (languageCode != null && !languageCode.isBlank()) {
      builder.queryParam("language-code", languageCode);
    }
    return builder.build(true).toUri();
  }
}
