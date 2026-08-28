package ai.swasthyavaani.api.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = PingController.class)
class PingControllerTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  void pingReturnsOk() {
    webTestClient
        .get()
        .uri("/api/v1/ping")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("ok")
        .jsonPath("$.service")
        .isEqualTo("swasthyavaani-api");
  }
}
