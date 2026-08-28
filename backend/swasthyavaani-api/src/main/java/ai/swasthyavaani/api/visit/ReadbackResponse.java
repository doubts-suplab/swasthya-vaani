package ai.swasthyavaani.api.visit;

/**
 * Spoken confirmation of a record. The worker hears {@code audioBase64} and sees {@code spokenText}
 * to confirm or correct.
 *
 * @param spokenText the text that was synthesized (target language, or English fallback)
 * @param officialText the formal registry-entry text (same as spokenText here)
 * @param targetLanguage BCP-47 language of the readback
 * @param audioBase64 base64-encoded audio (WAV) for playback
 * @param audioContentType MIME type of the audio
 * @param cached whether the audio came from the TTS cache (no vendor call/cost)
 */
public record ReadbackResponse(
    String spokenText,
    String officialText,
    String targetLanguage,
    String audioBase64,
    String audioContentType,
    boolean cached) {}
