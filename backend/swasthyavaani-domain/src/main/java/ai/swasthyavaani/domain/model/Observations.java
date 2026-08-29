package ai.swasthyavaani.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * What the worker observed and reported. {@code reportedSymptoms} are the worker's words, not a
 * diagnosis ({@code CLAUDE.md} §7.4).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Observations(
    Double weightKg,
    Double temperatureC,
    BloodPressure bloodPressure,
    Integer gestationWeeks,
    List<String> reportedSymptoms,
    String notes) {

  /**
   * Redacts free-text {@code notes} (potential PII) from logs; JSON is unaffected ({@code
   * CLAUDE.md} §7.2).
   */
  @Override
  public String toString() {
    return "Observations[weightKg="
        + weightKg
        + ", temperatureC="
        + temperatureC
        + ", bloodPressure="
        + bloodPressure
        + ", gestationWeeks="
        + gestationWeeks
        + ", reportedSymptoms="
        + reportedSymptoms
        + ", notes="
        + (notes == null ? "null" : "***")
        + "]";
  }
}
