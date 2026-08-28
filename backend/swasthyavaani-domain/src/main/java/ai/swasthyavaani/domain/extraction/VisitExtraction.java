package ai.swasthyavaani.domain.extraction;

import ai.swasthyavaani.domain.model.Actions;
import ai.swasthyavaani.domain.model.Beneficiary;
import ai.swasthyavaani.domain.model.Observations;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The strict-JSON subset the extraction model ({@code sarvam-m}) must emit from a transcript —
 * {@code beneficiary}, {@code observations}, {@code actions}, and {@code warnings}. See the
 * extraction contract and JSON Schema in {@code data-model.md} §5.
 *
 * <p>The backend wraps this with {@code visitId}, {@code workerId}, provenance, timestamps, and
 * sync fields to form the full {@link ai.swasthyavaani.domain.model.VisitRecord}. Validate every
 * extraction against the schema before persisting; on failure keep the visit as {@code DRAFT}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisitExtraction(
    Beneficiary beneficiary, Observations observations, Actions actions, List<String> warnings) {}
