package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.model.Actions;
import ai.swasthyavaani.domain.model.Beneficiary;
import ai.swasthyavaani.domain.model.Observations;
import ai.swasthyavaani.domain.model.VisitRecord;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Turns a {@link VisitRecord} into a concise, formal English summary — the basis for both the
 * spoken confirmation (translated + TTS) and the official registry-entry text ({@code roadmap.md}
 * T2-F06). Only states what the record contains; never adds interpretation ({@code CLAUDE.md}
 * §7.4).
 */
@Component
public class OfficialTextComposer {

  public String compose(VisitRecord record) {
    List<String> parts = new ArrayList<>();
    parts.add(who(record.beneficiary(), record.visitType().name()));

    Observations o = record.observations();
    if (o != null) {
      if (o.weightKg() != null) {
        parts.add("Weight " + o.weightKg() + " kilograms.");
      }
      if (o.temperatureC() != null) {
        parts.add("Temperature " + o.temperatureC() + " degrees Celsius.");
      }
      if (o.bloodPressure() != null && o.bloodPressure().systolic() != null) {
        parts.add(
            "Blood pressure "
                + o.bloodPressure().systolic()
                + " over "
                + o.bloodPressure().diastolic()
                + ".");
      }
      if (o.gestationWeeks() != null) {
        parts.add("Gestation " + o.gestationWeeks() + " weeks.");
      }
      if (o.reportedSymptoms() != null && !o.reportedSymptoms().isEmpty()) {
        parts.add("Reported symptoms: " + String.join(", ", o.reportedSymptoms()) + ".");
      }
    }

    Actions a = record.actions();
    if (a != null) {
      if (a.medicinesHandedOver() != null && !a.medicinesHandedOver().isEmpty()) {
        parts.add("Medicines handed over: " + String.join(", ", a.medicinesHandedOver()) + ".");
      }
      if (a.referral() != null && a.referral().referred()) {
        parts.add(
            "Referred to "
                + (a.referral().facility() != null ? a.referral().facility() : "a facility")
                + ".");
      }
      if (a.nextVisitDate() != null) {
        parts.add("Next visit on " + a.nextVisitDate() + ".");
      }
    }

    return String.join(" ", parts);
  }

  private static String who(Beneficiary b, String visitType) {
    var sb = new StringBuilder(visitType).append(" visit");
    if (b != null && b.name() != null) {
      sb.append(" for ").append(b.name());
      if (b.ageYears() != null) {
        sb.append(", age ").append(b.ageYears());
      }
    }
    sb.append('.');
    return sb.toString();
  }
}
