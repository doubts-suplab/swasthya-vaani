package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.model.VisitRecord;
import java.util.List;

/**
 * Result of the online happy path: the assembled {@link VisitRecord} (always {@code DRAFT} in Phase
 * 1) plus the raw transcript so the UI can show it and fall back to manual entry when the
 * extraction did not validate ({@code data-model.md} §5).
 *
 * @param visit the assembled draft record
 * @param transcript the raw STT transcript
 * @param extractionValid whether the extraction passed schema validation
 * @param validationMessages human-readable validation issues (empty when valid)
 */
public record VisitDraftResponse(
    VisitRecord visit,
    String transcript,
    boolean extractionValid,
    List<String> validationMessages) {}
