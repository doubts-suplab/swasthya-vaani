package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.enums.VisitType;
import java.util.List;

/**
 * Everything {@link VisitAssembler} needs to build a {@code DRAFT} record, independent of the
 * source (spoken transcript or OCR'd paper record). Provenance fields are passed in so the
 * assembler stays pure (no vendor config).
 *
 * @param visitId client id, or {@code null} to generate
 * @param workerId health-worker id (defaulted if null)
 * @param deviceId device id (defaulted if null)
 * @param visitType visit type, or {@code null} for GENERAL
 * @param villageName optional location
 * @param state optional state code
 * @param sourceLanguage BCP-47 source language
 * @param sttModel STT model id, or {@code null} for OCR-sourced records
 * @param extractionModel extraction model id
 * @param sourceText the raw transcript or OCR text (surfaced to the UI)
 * @param extraWarnings provenance notes to prepend (e.g. "ingested via OCR")
 * @param audioS3Key provenance pointer to the audio blob in S3, or {@code null}
 * @param transcriptS3Key provenance pointer to the transcript blob in S3, or {@code null}
 */
public record AssemblyInput(
    String visitId,
    String workerId,
    String deviceId,
    VisitType visitType,
    String villageName,
    String state,
    String sourceLanguage,
    String sttModel,
    String extractionModel,
    String sourceText,
    List<String> extraWarnings,
    String audioS3Key,
    String transcriptS3Key) {}
