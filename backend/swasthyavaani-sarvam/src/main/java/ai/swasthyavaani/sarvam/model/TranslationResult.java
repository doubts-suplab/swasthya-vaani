package ai.swasthyavaani.sarvam.model;

/**
 * The result of a translation call.
 *
 * @param translatedText the translated text
 * @param requestId Sarvam request id
 */
public record TranslationResult(String translatedText, String requestId) {}
