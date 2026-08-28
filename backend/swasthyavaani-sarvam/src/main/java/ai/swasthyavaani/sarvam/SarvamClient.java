package ai.swasthyavaani.sarvam;

import ai.swasthyavaani.domain.extraction.VisitExtraction;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.SpeechRequest;
import ai.swasthyavaani.sarvam.model.SpeechResult;
import ai.swasthyavaani.sarvam.model.TranscriptionRequest;
import ai.swasthyavaani.sarvam.model.TranscriptionResult;
import ai.swasthyavaani.sarvam.model.TranslationRequest;
import ai.swasthyavaani.sarvam.model.TranslationResult;
import reactor.core.publisher.Mono;

/**
 * The single seam to every Sarvam AI capability. All vendor access goes through this interface — no
 * Sarvam SDK types, endpoints, or model ids leak into controllers or the UI ({@code CLAUDE.md} §6).
 * There is exactly one implementation.
 *
 * <p>Every method applies the resilience policy in {@code sarvam-integration.md} §9 (timeout,
 * bounded retry-with-backoff on retryable failures) and fails with {@link SarvamException} so
 * callers can trigger the degraded/offline fallbacks in {@code architecture.md} §6.
 */
public interface SarvamClient {

  /**
   * Batch / fallback speech-to-text (Saaras v3). Used when the realtime socket is unavailable.
   *
   * @param request audio + language/mode hints
   * @return the transcript and detected language
   */
  Mono<TranscriptionResult> transcribe(TranscriptionRequest request);

  /**
   * Turn a transcript into the strict-JSON {@link VisitExtraction} via the extraction model ({@code
   * sarvam-m}). The implementation enforces JSON-only output; schema validation against {@code
   * data-model.md} §5 is the caller's responsibility before persisting.
   *
   * @param request transcript + minimal, non-PII context
   * @return the extracted (unwrapped) visit fields
   */
  Mono<VisitExtraction> extractVisit(ExtractionRequest request);

  /**
   * Confirmation readback via Bulbul TTS. Callers should cache results by normalised text — TTS is
   * metered ({@code sarvam-integration.md} §5, §10).
   *
   * @param request text + target language + optional speaker
   * @return decoded (playable) audio bytes
   */
  Mono<SpeechResult> synthesizeSpeech(SpeechRequest request);

  /**
   * Translate text for the formal registry entry (Sarvam-Translate).
   *
   * @param request input + source/target languages
   * @return the translated text
   */
  Mono<TranslationResult> translate(TranslationRequest request);
}
