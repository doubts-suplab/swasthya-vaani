package ai.swasthyavaani.api.visit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.SpeechRequest;
import ai.swasthyavaani.sarvam.model.SpeechResult;
import ai.swasthyavaani.sarvam.model.TranslationRequest;
import ai.swasthyavaani.sarvam.model.TranslationResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VisitReadbackServiceTest {

  @Mock private SarvamClient sarvam;

  private VisitReadbackService service;

  @BeforeEach
  void setUp() {
    var props =
        new SarvamProperties(
            "test-key",
            "https://api.sarvam.ai",
            "wss://api.sarvam.ai/speech-to-text/ws",
            new SarvamProperties.Languages("bn-IN", List.of("bn-IN", "en-IN")),
            new SarvamProperties.Models(
                "saaras:v3", "saaras:v3-realtime", "sarvam-m", "bulbul:v3", "sarvam-translate:v1"),
            new SarvamProperties.Tts("shubh", 22050),
            new SarvamProperties.Timeouts(Duration.ofSeconds(3), Duration.ofSeconds(30)),
            new SarvamProperties.Retry(3, Duration.ofMillis(500)));
    service = new VisitReadbackService(sarvam, props, new OfficialTextComposer());
  }

  @Test
  void translatesThenSynthesizesAndCachesAudio() {
    var audio = "wav".getBytes(StandardCharsets.UTF_8);
    when(sarvam.translate(any(TranslationRequest.class)))
        .thenReturn(Mono.just(new TranslationResult("বাংলা টেক্সট", "r-t")));
    when(sarvam.synthesizeSpeech(any(SpeechRequest.class)))
        .thenReturn(Mono.just(new SpeechResult(audio, "r-s")));

    // First call synthesizes...
    StepVerifier.create(service.readback(VisitTestData.draft(), "bn-IN"))
        .assertNext(
            r -> {
              org.junit.jupiter.api.Assertions.assertFalse(r.cached());
              org.junit.jupiter.api.Assertions.assertEquals("bn-IN", r.targetLanguage());
              org.junit.jupiter.api.Assertions.assertEquals("বাংলা টেক্সট", r.spokenText());
            })
        .verifyComplete();

    // ...second identical call is served from the cache (no extra TTS call).
    StepVerifier.create(service.readback(VisitTestData.draft(), "bn-IN"))
        .assertNext(r -> org.junit.jupiter.api.Assertions.assertTrue(r.cached()))
        .verifyComplete();

    verify(sarvam, times(1)).synthesizeSpeech(any(SpeechRequest.class));
  }

  @Test
  void fallsBackToEnglishWhenTranslationFails() {
    when(sarvam.translate(any(TranslationRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("translate down")));
    when(sarvam.synthesizeSpeech(any(SpeechRequest.class)))
        .thenReturn(Mono.just(new SpeechResult("wav".getBytes(StandardCharsets.UTF_8), "r")));

    StepVerifier.create(service.readback(VisitTestData.draft(), "bn-IN"))
        .assertNext(
            r ->
                org.junit.jupiter.api.Assertions.assertTrue(
                    r.spokenText().contains("Rekha Das"), r.spokenText()))
        .verifyComplete();
  }

  @Test
  void skipsTranslationForEnglishTarget() {
    when(sarvam.synthesizeSpeech(any(SpeechRequest.class)))
        .thenReturn(Mono.just(new SpeechResult("wav".getBytes(StandardCharsets.UTF_8), "r")));

    StepVerifier.create(service.readback(VisitTestData.draft(), "en-IN"))
        .assertNext(
            r ->
                org.junit.jupiter.api.Assertions.assertTrue(
                    r.spokenText().contains("ANC visit for Rekha Das")))
        .verifyComplete();

    verify(sarvam, times(0)).translate(any(TranslationRequest.class));
  }
}
