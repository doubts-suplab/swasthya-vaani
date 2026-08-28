package ai.swasthyavaani.sarvam.model;

/**
 * A translation request ({@code POST /translate}) to produce the formal-language registry entry
 * ({@code sarvam-integration.md} §7).
 *
 * @param input source text
 * @param sourceLanguageCode BCP-47 source, or {@code null} to auto-detect
 * @param targetLanguageCode BCP-47 target
 */
public record TranslationRequest(
    String input, String sourceLanguageCode, String targetLanguageCode) {}
