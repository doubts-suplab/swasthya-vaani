package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.enums.VisitType;

/**
 * Everything the transcription pipeline needs for one visit. Audio + optional context; the audio
 * itself is never persisted here (S3 blob storage is Phase 3).
 *
 * @param audio raw audio bytes
 * @param filename original filename (codec hinting)
 * @param contentType audio MIME type
 * @param languageCode BCP-47 hint, or {@code null} to auto-detect
 * @param workerId health-worker id (defaulted if absent)
 * @param deviceId device id (defaulted if absent)
 * @param visitType type of visit, or {@code null} for {@code GENERAL}
 * @param villageName optional location context
 * @param state optional state code (e.g. {@code WB})
 * @param visitId client-supplied idempotency key, or {@code null} to generate one (Phase 1)
 */
public record TranscribeCommand(
    byte[] audio,
    String filename,
    String contentType,
    String languageCode,
    String workerId,
    String deviceId,
    VisitType visitType,
    String villageName,
    String state,
    String visitId) {}
