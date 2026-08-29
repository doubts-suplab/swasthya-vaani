package ai.swasthyavaani.api.ocr;

import ai.swasthyavaani.domain.enums.VisitType;

/**
 * Everything the OCR-ingest pipeline needs for one photographed record (Phase 4).
 *
 * @param image raw image bytes (JPEG/PNG/PDF page)
 * @param filename original filename
 * @param contentType image MIME type
 * @param languageCode BCP-47 hint, or {@code null} to auto-detect
 * @param workerId health-worker id (defaulted if absent)
 * @param deviceId device id (defaulted if absent)
 * @param visitType type of visit, or {@code null} for GENERAL
 * @param villageName optional location context
 * @param state optional state code
 * @param visitId client id, or {@code null} to generate
 */
public record OcrCommand(
    byte[] image,
    String filename,
    String contentType,
    String languageCode,
    String workerId,
    String deviceId,
    VisitType visitType,
    String villageName,
    String state,
    String visitId) {}
