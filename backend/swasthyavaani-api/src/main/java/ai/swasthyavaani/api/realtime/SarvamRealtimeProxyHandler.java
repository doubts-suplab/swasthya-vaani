package ai.swasthyavaani.api.realtime;

import ai.swasthyavaani.sarvam.config.SarvamProperties;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Proxies a browser's realtime STT WebSocket to Sarvam's {@code saaras:v3-realtime} channel, so the
 * subscription key stays server-side (client ⇄ this API ⇄ Sarvam). Audio frames flow up; partial
 * transcript frames flow down.
 *
 * <p><strong>Pending live verification.</strong> The realtime channel path and message framing are
 * marked "assumption — verify" in {@code sarvam-integration.md} §4; the URI/key-safety logic is
 * unit-tested, but the end-to-end frame protocol must be confirmed against live Sarvam before this
 * is relied on in the field. Until then the batch STT path (Phase 1) is the supported route.
 */
public class SarvamRealtimeProxyHandler implements WebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(SarvamRealtimeProxyHandler.class);
  private static final int DEFAULT_SAMPLE_RATE = 16000;

  private final WebSocketClient upstreamClient;
  private final SarvamProperties props;

  public SarvamRealtimeProxyHandler(WebSocketClient upstreamClient, SarvamProperties props) {
    this.upstreamClient = upstreamClient;
    this.props = props;
  }

  @Override
  public Mono<Void> handle(WebSocketSession clientSession) {
    String languageCode = queryParam(clientSession, "language");
    URI upstreamUri = RealtimeUri.build(props, languageCode, DEFAULT_SAMPLE_RATE);

    var headers = new HttpHeaders();
    if (props.apiKey() != null && !props.apiKey().isBlank()) {
      headers.add("Api-Subscription-Key", props.apiKey());
    }

    return upstreamClient.execute(
        upstreamUri,
        headers,
        upstreamSession -> {
          // Browser audio -> Sarvam
          Mono<Void> up =
              upstreamSession.send(clientSession.receive().map(m -> copy(upstreamSession, m)));
          // Sarvam transcripts -> browser
          Mono<Void> down =
              clientSession.send(upstreamSession.receive().map(m -> copy(clientSession, m)));
          return Mono.zip(up, down)
              .then()
              .doOnError(e -> log.warn("Realtime proxy error: {}", e.getClass().getSimpleName()));
        });
  }

  /**
   * Copy a message onto the target session, preserving its type (binary audio / text transcript).
   */
  private static WebSocketMessage copy(WebSocketSession target, WebSocketMessage message) {
    var payload = message.getPayload();
    byte[] bytes = new byte[payload.readableByteCount()];
    payload.read(bytes);
    return new WebSocketMessage(message.getType(), target.bufferFactory().wrap(bytes));
  }

  private static String queryParam(WebSocketSession session, String name) {
    return UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
        .build()
        .getQueryParams()
        .getFirst(name);
  }
}
