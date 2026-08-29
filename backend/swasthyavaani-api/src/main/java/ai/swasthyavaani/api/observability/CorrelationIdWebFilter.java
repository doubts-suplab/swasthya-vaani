package ai.swasthyavaani.api.observability;

import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Assigns a correlation id to every request so a single visit can be traced end-to-end (T7-F06).
 * Honours an inbound {@code X-Correlation-Id}, otherwise generates one; echoes it on the response
 * and places it in the Reactor context for downstream logging. Carries no PII.
 */
@Component
public class CorrelationIdWebFilter implements WebFilter, Ordered {

  public static final String HEADER = "X-Correlation-Id";
  public static final String CONTEXT_KEY = "correlationId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String existing = exchange.getRequest().getHeaders().getFirst(HEADER);
    String correlationId =
        (existing != null && !existing.isBlank()) ? existing : UUID.randomUUID().toString();
    exchange.getResponse().getHeaders().set(HEADER, correlationId);
    return chain.filter(exchange).contextWrite(ctx -> ctx.put(CONTEXT_KEY, correlationId));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
