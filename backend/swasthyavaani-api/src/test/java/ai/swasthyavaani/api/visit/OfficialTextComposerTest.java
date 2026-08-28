package ai.swasthyavaani.api.visit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OfficialTextComposerTest {

  private final OfficialTextComposer composer = new OfficialTextComposer();

  @Test
  void composesFormalSummaryFromPresentFields() {
    String text = composer.compose(VisitTestData.draft());

    assertTrue(text.contains("ANC visit for Rekha Das"), text);
    assertTrue(text.contains("age 24"), text);
    assertTrue(text.contains("Weight 52.5 kilograms."), text);
    assertTrue(text.contains("Gestation 28 weeks."), text);
    assertTrue(text.contains("fever since yesterday"), text);
    assertTrue(text.contains("IFA tablets"), text);
    assertTrue(text.contains("Next visit on 2026-09-11."), text);
  }
}
