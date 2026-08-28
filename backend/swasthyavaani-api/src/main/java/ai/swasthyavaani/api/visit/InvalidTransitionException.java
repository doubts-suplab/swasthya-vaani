package ai.swasthyavaani.api.visit;

/** Raised when a confirmation-status transition is not allowed (mapped to HTTP 409). */
public class InvalidTransitionException extends RuntimeException {
  public InvalidTransitionException(String message) {
    super(message);
  }
}
