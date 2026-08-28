package ai.swasthyavaani.api.health;

import ai.swasthyavaani.sarvam.config.SarvamProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Reports Sarvam configuration readiness at {@code /actuator/health}. Phase 0 only checks that a
 * key and models are configured (no live call, no cost, no PII). A later phase may add a cheap,
 * cached liveness probe against Sarvam.
 *
 * <p>Never logs or exposes the API key — only whether one is present.
 */
@Component("sarvam")
public class SarvamHealthIndicator implements ReactiveHealthIndicator {

  private final SarvamProperties props;

  public SarvamHealthIndicator(SarvamProperties props) {
    this.props = props;
  }

  @Override
  public Mono<Health> health() {
    boolean keyPresent = props.apiKey() != null && !props.apiKey().isBlank();
    var builder = keyPresent ? Health.up() : Health.status("NO_API_KEY");
    return Mono.just(
        builder
            .withDetail("apiKeyConfigured", keyPresent)
            .withDetail("baseUrl", props.baseUrl())
            .withDetail("sttModel", props.models().stt())
            .withDetail("extractionModel", props.models().extraction())
            .withDetail("ttsModel", props.models().tts())
            .withDetail("supportedLanguages", props.languages().supported())
            .build());
  }
}
