package ai.swasthyavaani.domain.model;

import ai.swasthyavaani.domain.enums.BeneficiaryCategory;
import ai.swasthyavaani.domain.enums.Gender;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The person visited. {@code name} is high-PII: encrypt at rest, never log ({@code CLAUDE.md}
 * §7.2). Use a local {@code beneficiaryRef}, never Aadhaar/government id.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Beneficiary(
    String beneficiaryRef,
    String name,
    BeneficiaryCategory category,
    Gender gender,
    Integer ageYears) {

  /**
   * Redacts the high-PII {@code name} so it never leaks into logs ({@code CLAUDE.md} §7.2). JSON
   * serialization uses the fields directly and is unaffected — only {@code toString()} (what
   * logging frameworks call) is scrubbed.
   */
  @Override
  public String toString() {
    return "Beneficiary[beneficiaryRef="
        + beneficiaryRef
        + ", name="
        + (name == null ? "null" : "***")
        + ", category="
        + category
        + ", gender="
        + gender
        + ", ageYears="
        + ageYears
        + "]";
  }
}
