package ai.swasthyavaani.sarvam.model;

/**
 * A batch/fallback STT request ({@code POST /speech-to-text}). {@code languageCode} is BCP-47 (e.g.
 * {@code bn-IN}) or {@code null}/{@code "unknown"} for auto-detect.
 *
 * @param audio raw audio bytes
 * @param filename original filename (used for the multipart part + codec hinting)
 * @param contentType MIME type of the audio (e.g. {@code audio/wav}, {@code audio/ogg})
 * @param languageCode BCP-47 code, or {@code null} to auto-detect
 * @param mode output mode; defaults applied by the client when {@code null}
 */
public record TranscriptionRequest(
    byte[] audio, String filename, String contentType, String languageCode, SttMode mode) {}
