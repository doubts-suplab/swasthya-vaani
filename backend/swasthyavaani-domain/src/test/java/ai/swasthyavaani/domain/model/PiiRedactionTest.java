package ai.swasthyavaani.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.swasthyavaani.domain.enums.BeneficiaryCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

/** No PII in logs (CLAUDE.md §7.2): toString() must not expose the name or free-text notes. */
class PiiRedactionTest {

  @Test
  void beneficiaryToStringRedactsName() {
    var b = new Beneficiary("BEN-1", "Rekha Das", BeneficiaryCategory.PREGNANT_WOMAN, null, 24);
    var s = b.toString();
    assertFalse(s.contains("Rekha Das"), s);
    assertTrue(s.contains("***"), s);
    // Non-PII fields remain visible for debugging.
    assertTrue(s.contains("BEN-1"));
    assertTrue(s.contains("24"));
  }

  @Test
  void observationsToStringRedactsNotes() {
    var o = new Observations(52.5, null, null, 28, List.of("fever"), "sensitive free text");
    var s = o.toString();
    assertFalse(s.contains("sensitive free text"), s);
    assertTrue(s.contains("52.5"));
  }
}
