package ai.swasthyavaani.sarvam.model;

/**
 * A field-extraction request: turn a transcript into the strict-JSON {@code VisitExtraction}
 * ({@code sarvam-integration.md} §6). {@code context} carries minimal, non-PII hints (e.g. the
 * worker's default location, expected language) to steer the model — never patient identifiers.
 *
 * @param transcript the STT output to extract from
 * @param context minimal extraction context, or {@code null}
 */
public record ExtractionRequest(String transcript, String context) {}
