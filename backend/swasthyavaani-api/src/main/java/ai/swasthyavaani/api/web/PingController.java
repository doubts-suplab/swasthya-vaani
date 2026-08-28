package ai.swasthyavaani.api.web;

import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Lightweight liveness/readiness ping. Actuator ({@code /actuator/health}) carries deep health. */
@RestController
@RequestMapping("/api/v1")
public class PingController {

  private final String version;

  public PingController(@Value("${spring.application.version:0.1.0-SNAPSHOT}") String version) {
    this.version = version;
  }

  @GetMapping("/ping")
  public Mono<PingResponse> ping() {
    return Mono.just(
        new PingResponse("ok", "swasthyavaani-api", version, OffsetDateTime.now().toString()));
  }

  /** Ping payload. */
  public record PingResponse(String status, String service, String version, String time) {}
}
