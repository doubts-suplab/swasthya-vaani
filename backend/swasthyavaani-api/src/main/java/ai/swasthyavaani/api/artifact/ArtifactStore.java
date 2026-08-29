package ai.swasthyavaani.api.artifact;

import reactor.core.publisher.Mono;

/**
 * Stores a visit's binary/text artifacts (audio, transcript, OCR) out of the record and in S3
 * ({@code data-model.md} §2, §8). {@link #enabled()} lets the pipeline set provenance S3 keys only
 * when a real store is wired (the {@code aws} profile); the default {@code NoOp} store keeps the
 * PoC blob-free.
 */
public interface ArtifactStore {

  /** Whether artifacts are actually persisted (true only under the {@code aws} profile). */
  boolean enabled();

  /** Upload bytes to the given key. Fire-and-forget from the caller's perspective. */
  Mono<Void> put(String key, byte[] data, String contentType);
}
