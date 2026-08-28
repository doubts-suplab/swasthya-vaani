package ai.swasthyavaani.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * How the record was produced, so a human can trust or correct it. {@code warnings} carry
 * low-confidence or inferred fields. S3 keys are pointers only — blobs live in S3, not in the
 * record ({@code data-model.md} §2, §8).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Provenance(
    String sourceLanguage,
    String sttModel,
    String extractionModel,
    Double extractionConfidence,
    List<String> warnings,
    String audioS3Key,
    String transcriptS3Key) {}
