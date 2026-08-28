package ai.swasthyavaani.sarvam.model;

/**
 * The transcription returned by {@code POST /speech-to-text}.
 *
 * @param transcript the recognised text
 * @param languageCode detected/echoed BCP-47 language
 * @param languageProbability confidence of language detection, 0–1
 * @param requestId Sarvam request id (for support/traceability — not PII)
 */
public record TranscriptionResult(
    String transcript, String languageCode, Double languageProbability, String requestId) {}
