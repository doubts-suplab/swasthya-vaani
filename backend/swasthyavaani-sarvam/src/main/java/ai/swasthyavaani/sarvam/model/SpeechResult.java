package ai.swasthyavaani.sarvam.model;

/**
 * Synthesized speech, already base64-decoded into playable bytes ({@code sarvam-integration.md}
 * §5).
 *
 * @param audio decoded audio (WAV) bytes
 * @param requestId Sarvam request id
 */
public record SpeechResult(byte[] audio, String requestId) {}
