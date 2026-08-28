package ai.swasthyavaani.api.validation;

import java.util.List;

/**
 * Outcome of validating an extraction against the schema. When {@code valid} is false the visit is
 * kept as {@code DRAFT} and {@code messages} explain why, so a worker can review/correct ({@code
 * data-model.md} §5).
 */
public record ValidationResult(boolean valid, List<String> messages) {

  public static ValidationResult ok() {
    return new ValidationResult(true, List.of());
  }

  public static ValidationResult invalid(List<String> messages) {
    return new ValidationResult(false, List.copyOf(messages));
  }
}
