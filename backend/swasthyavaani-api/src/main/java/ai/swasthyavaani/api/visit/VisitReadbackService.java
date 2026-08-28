package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.model.VisitRecord;
import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.SpeechRequest;
import ai.swasthyavaani.sarvam.model.TranslationRequest;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Phase 2 confirmation readback: compose the official text, translate it to the worker's language
 * (Sarvam-Translate, T2-F06 — degrades to English if translation is unavailable), synthesize it
 * with Bulbul, and speak it back. TTS is metered, so audio is cached by normalised spoken text
 * ({@code sarvam-integration.md} §5, §10).
 */
@Service
public class VisitReadbackService {

  private static final Logger log = LoggerFactory.getLogger(VisitReadbackService.class);
  private static final String AUDIO_CONTENT_TYPE = "audio/wav";

  private final SarvamClient sarvam;
  private final SarvamProperties props;
  private final OfficialTextComposer composer;
  private final Cache<String, byte[]> ttsCache;

  public VisitReadbackService(
      SarvamClient sarvam, SarvamProperties props, OfficialTextComposer composer) {
    this.sarvam = sarvam;
    this.props = props;
    this.composer = composer;
    this.ttsCache =
        Caffeine.newBuilder().maximumSize(500).expireAfterWrite(Duration.ofHours(6)).build();
  }

  public Mono<ReadbackResponse> readback(VisitRecord record, String targetLanguageOrNull) {
    String targetLanguage =
        targetLanguageOrNull != null && !targetLanguageOrNull.isBlank()
            ? targetLanguageOrNull
            : props.languages().defaultTarget();
    String english = composer.compose(record);

    return spokenText(english, targetLanguage)
        .flatMap(spoken -> synthesize(spoken, targetLanguage));
  }

  /**
   * Translate to the target language; on any failure fall back to the English text (never fatal).
   */
  private Mono<String> spokenText(String english, String targetLanguage) {
    if (targetLanguage.toLowerCase(Locale.ROOT).startsWith("en")) {
      return Mono.just(english);
    }
    return sarvam
        .translate(new TranslationRequest(english, "en-IN", targetLanguage))
        .map(result -> result.translatedText() != null ? result.translatedText() : english)
        .onErrorResume(
            e -> {
              log.warn(
                  "Translation unavailable ({}); reading back in English",
                  e.getClass().getSimpleName());
              return Mono.just(english);
            });
  }

  private Mono<ReadbackResponse> synthesize(String spoken, String targetLanguage) {
    String key = cacheKey(spoken, targetLanguage);
    byte[] cached = ttsCache.getIfPresent(key);
    if (cached != null) {
      return Mono.just(response(spoken, targetLanguage, cached, true));
    }
    return sarvam
        .synthesizeSpeech(new SpeechRequest(spoken, targetLanguage, null))
        .map(
            speech -> {
              ttsCache.put(key, speech.audio());
              return response(spoken, targetLanguage, speech.audio(), false);
            });
  }

  private static ReadbackResponse response(
      String spoken, String targetLanguage, byte[] audio, boolean cached) {
    return new ReadbackResponse(
        spoken,
        spoken,
        targetLanguage,
        Base64.getEncoder().encodeToString(audio),
        AUDIO_CONTENT_TYPE,
        cached);
  }

  private static String cacheKey(String spoken, String targetLanguage) {
    return targetLanguage + "|" + spoken.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }
}
