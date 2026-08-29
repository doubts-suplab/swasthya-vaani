package ai.swasthyavaani.api.artifact;

import ai.swasthyavaani.api.store.AwsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3-backed {@link ArtifactStore} (active under the {@code aws} profile). Uploads to the private,
 * SSE, TLS-only artifacts bucket provisioned by the CDK stack. Non-blocking; keys per {@code
 * data-model.md} §8. Compile-verified here — integration is the deploy step (see runbook).
 */
@Component
@Profile("aws")
public class S3ArtifactStore implements ArtifactStore {

  private static final Logger log = LoggerFactory.getLogger(S3ArtifactStore.class);

  private final S3AsyncClient s3;
  private final AwsProperties props;

  public S3ArtifactStore(S3AsyncClient s3, AwsProperties props) {
    this.s3 = s3;
    this.props = props;
  }

  @Override
  public boolean enabled() {
    return true;
  }

  @Override
  public Mono<Void> put(String key, byte[] data, String contentType) {
    var request =
        PutObjectRequest.builder()
            .bucket(props.artifactsBucket())
            .key(key)
            .contentType(contentType)
            .build();
    return Mono.fromFuture(s3.putObject(request, AsyncRequestBody.fromBytes(data)))
        .doOnError(
            e ->
                log.warn(
                    "Artifact upload failed for key {} ({})", key, e.getClass().getSimpleName()))
        .then();
  }
}
