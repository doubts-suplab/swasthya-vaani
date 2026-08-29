package ai.swasthyavaani.api.ocr;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ai.swasthyavaani.api.validation.VisitExtractionValidator;
import ai.swasthyavaani.api.visit.VisitAssembler;
import ai.swasthyavaani.domain.enums.BeneficiaryCategory;
import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.extraction.VisitExtraction;
import ai.swasthyavaani.domain.model.Actions;
import ai.swasthyavaani.domain.model.Beneficiary;
import ai.swasthyavaani.domain.model.Observations;
import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.SarvamException;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.OcrRequest;
import ai.swasthyavaani.sarvam.model.OcrResult;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VisitOcrServiceTest {

  @Mock private SarvamClient sarvam;
  private VisitOcrService service;

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
    var assembler =
        new VisitAssembler(
            new VisitExtractionValidator(JsonMapper.builder().findAndAddModules().build()),
            Clock.fixed(Instant.parse("2026-08-28T09:14:00Z"), ZoneId.of("Asia/Kolkata")));
    service = new VisitOcrService(sarvam, props, assembler);
  }

  private OcrCommand command() {
    return new OcrCommand(
        "img".getBytes(StandardCharsets.UTF_8),
        "page.jpg",
        "image/jpeg",
        "bn-IN",
        "ASHA-WB-1",
        "dev-1",
        null,
        "Amtala",
        "WB",
        null);
  }

  @Test
  void digitisesThenExtractsToDraftWithOcrProvenance() {
    when(sarvam.ocr(any(OcrRequest.class)))
        .thenReturn(Mono.just(new OcrResult("Rekha Das, 24, weight 52.5 kg", "r-1")));
    when(sarvam.extractVisit(any(ExtractionRequest.class)))
        .thenReturn(
            Mono.just(
                new VisitExtraction(
                    new Beneficiary(
                        null, "Rekha Das", BeneficiaryCategory.PREGNANT_WOMAN, null, 24),
                    new Observations(52.5, null, null, null, null, null),
                    new Actions(null, null, null),
                    List.of())));

    StepVerifier.create(service.ingestPhoto(command()))
        .assertNext(
            response -> {
              var visit = response.visit();
              Assertions.assertTrue(response.extractionValid());
              Assertions.assertEquals(ConfirmationStatus.DRAFT, visit.confirmationStatus());
              Assertions.assertEquals("Rekha Das", visit.beneficiary().name());
              Assertions.assertEquals("Rekha Das, 24, weight 52.5 kg", response.transcript());
              // OCR-sourced: no STT model, and a provenance note that it came from a photo.
              Assertions.assertNull(visit.provenance().sttModel());
              Assertions.assertEquals("sarvam-m", visit.provenance().extractionModel());
              Assertions.assertTrue(
                  visit.provenance().warnings().stream().anyMatch(w -> w.contains("OCR")));
            })
        .verifyComplete();
  }

  @Test
  void extractionFailureDegradesToManualEntry() {
    when(sarvam.ocr(any(OcrRequest.class)))
        .thenReturn(Mono.just(new OcrResult("blurry text", "r-2")));
    when(sarvam.extractVisit(any(ExtractionRequest.class)))
        .thenReturn(Mono.error(new SarvamException("bad json", 422)));

    StepVerifier.create(service.ingestPhoto(command()))
        .assertNext(
            response -> {
              Assertions.assertFalse(response.extractionValid());
              Assertions.assertEquals("blurry text", response.transcript());
              Assertions.assertNull(response.visit().beneficiary());
            })
        .verifyComplete();
  }
}
