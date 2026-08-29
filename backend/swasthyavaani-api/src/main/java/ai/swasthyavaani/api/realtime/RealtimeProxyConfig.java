package ai.swasthyavaani.api.realtime;

import ai.swasthyavaani.sarvam.config.SarvamProperties;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 * Registers the realtime STT proxy at {@code /ws/stt}. A browser connects here; the server relays
 * to Sarvam with the key in a header (see {@link SarvamRealtimeProxyHandler}). Ordered ahead of
 * annotated-controller mappings so the WS upgrade is handled before REST routing.
 */
@Configuration
public class RealtimeProxyConfig {

  static final String PROXY_PATH = "/ws/stt";

  @Bean
  WebSocketClient sarvamUpstreamWebSocketClient() {
    return new ReactorNettyWebSocketClient();
  }

  @Bean
  SarvamRealtimeProxyHandler sarvamRealtimeProxyHandler(
      WebSocketClient sarvamUpstreamWebSocketClient, SarvamProperties props) {
    return new SarvamRealtimeProxyHandler(sarvamUpstreamWebSocketClient, props);
  }

  @Bean
  HandlerMapping realtimeHandlerMapping(SarvamRealtimeProxyHandler handler) {
    var mapping = new SimpleUrlHandlerMapping();
    mapping.setUrlMap(Map.of(PROXY_PATH, handler));
    mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return mapping;
  }

  @Bean
  WebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new WebSocketHandlerAdapter();
  }
}
