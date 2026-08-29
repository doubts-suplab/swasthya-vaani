package ai.swasthyavaani.sarvam.model;

/**
 * An OCR request for Sarvam Document AI (Sarvam Vision) — digitise a photographed paper register /
 * MCP card into text ({@code sarvam-integration.md} §8).
 *
 * @param image raw image bytes (JPEG/PNG/PDF page)
 * @param filename original filename
 * @param contentType image MIME type (e.g. {@code image/jpeg})
 * @param languageCode BCP-47 hint, or {@code null} to auto-detect
 */
public record OcrRequest(byte[] image, String filename, String contentType, String languageCode) {}
