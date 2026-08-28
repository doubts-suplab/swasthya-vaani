package ai.swasthyavaani.sarvam;

/**
 * Raised when a Sarvam call fails after retries or returns a non-retryable error. Callers use this
 * to trigger the offline/degraded fallbacks in {@code architecture.md} §6. Never carries PII.
 */
public class SarvamException extends RuntimeException {

  private final int status;

  public SarvamException(String message, int status) {
    super(message);
    this.status = status;
  }

  public SarvamException(String message, int status, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  /** Upstream HTTP status, or {@code 0} for transport/timeout failures. */
  public int status() {
    return status;
  }
}
