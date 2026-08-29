package ai.swasthyavaani.api.artifact;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Default artifact store: does nothing, so the PoC runs without S3. Replaced under {@code aws}. */
@Component
@Profile("!aws")
public class NoOpArtifactStore implements ArtifactStore {

  @Override
  public boolean enabled() {
    return false;
  }

  @Override
  public Mono<Void> put(String key, byte[] data, String contentType) {
    return Mono.empty();
  }
}
