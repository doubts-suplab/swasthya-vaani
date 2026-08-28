package ai.swasthyavaani.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

/**
 * What the worker did during the visit. {@code medicinesHandedOver} is what was physically given —
 * never medication <em>advice</em> ({@code CLAUDE.md} §7.4).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Actions(
    List<String> medicinesHandedOver, Referral referral, LocalDate nextVisitDate) {}
