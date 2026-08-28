package ai.swasthyavaani.sarvam.model;

/**
 * A text-to-speech request ({@code POST /text-to-speech}, Bulbul). {@code speaker} defaults to the
 * configured voice when {@code null}.
 *
 * @param text text to synthesize (code-mixed allowed)
 * @param targetLanguageCode BCP-47 code (e.g. {@code bn-IN})
 * @param speaker voice id, or {@code null} for the configured default
 */
public record SpeechRequest(String text, String targetLanguageCode, String speaker) {}
