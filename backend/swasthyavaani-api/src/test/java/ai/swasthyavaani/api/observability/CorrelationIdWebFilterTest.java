package ai.swasthyavaani.api.observability;

import ai.swasthyavaani.api.web.PingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = PingController.class)
@Import(CorrelationIdWebFilter.class)
class CorrelationIdWebFilterTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  void generatesACorrelationIdWhenAbsent() {
    webTestClient
        .get()
        .uri("/api/v1/ping")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists(CorrelationIdWebFilter.HEADER);
  }

  @Test
  void echoesAnInboundCorrelationId() {
    webTestClient
        .get()
        .uri("/api/v1/ping")
        .header(CorrelationIdWebFilter.HEADER, "trace-123")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(CorrelationIdWebFilter.HEADER, "trace-123");
  }
}
