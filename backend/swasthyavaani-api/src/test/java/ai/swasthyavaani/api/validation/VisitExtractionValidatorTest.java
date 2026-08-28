package ai.swasthyavaani.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.swasthyavaani.domain.enums.BeneficiaryCategory;
import ai.swasthyavaani.domain.extraction.VisitExtraction;
import ai.swasthyavaani.domain.model.Actions;
import ai.swasthyavaani.domain.model.Beneficiary;
import ai.swasthyavaani.domain.model.Observations;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisitExtractionValidatorTest {

  private final VisitExtractionValidator validator =
      new VisitExtractionValidator(JsonMapper.builder().findAndAddModules().build());

  @Test
  void acceptsAWellFormedExtraction() {
    var extraction =
        new VisitExtraction(
            new Beneficiary(null, "Rekha Das", BeneficiaryCategory.PREGNANT_WOMAN, null, 24),
            new Observations(52.5, 37.8, null, 28, List.of("fever"), null),
            new Actions(List.of("IFA tablets"), null, null),
            List.of());

    assertTrue(validator.validate(extraction).valid());
  }

  @Test
  void rejectsAgeOutOfRange() {
    var extraction =
        new VisitExtraction(
            new Beneficiary(null, "Test", null, null, 200), // schema max is 120
            new Observations(null, null, null, null, null, null),
            new Actions(null, null, null),
            List.of());

    var result = validator.validate(extraction);
    assertFalse(result.valid());
    assertFalse(result.messages().isEmpty());
  }
}
