package ai.swasthyavaani.api.error;

import ai.swasthyavaani.sarvam.SarvamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain/vendor failures into RFC-7807 problem responses. Sarvam being unavailable is an
 * upstream (502) condition — the client should fall back to record-and-retry ({@code
 * architecture.md} §6), not treat it as its own bug. Messages never carry PII.
 */
@RestControllerAdvice
public class GlobalErrorHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

  @ExceptionHandler(SarvamException.class)
  public ProblemDetail handleSarvam(SarvamException ex) {
    log.warn("Sarvam upstream failure (status {})", ex.status());
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY, "The speech service is temporarily unavailable. Please retry.");
    problem.setTitle("Upstream speech service error");
    problem.setProperty("upstreamStatus", ex.status());
    return problem;
  }
}
