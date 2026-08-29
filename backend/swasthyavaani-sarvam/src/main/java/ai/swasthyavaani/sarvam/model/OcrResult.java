package ai.swasthyavaani.sarvam.model;

/**
 * The digitised text of a photographed record.
 *
 * @param text the extracted text (native-script or code-mixed), layout flattened to reading order
 * @param requestId Sarvam request id
 */
public record OcrResult(String text, String requestId) {}
