package ai.swasthyavaani.api.visit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ai.swasthyavaani.api.validation.VisitExtractionValidator;
import ai.swasthyavaani.domain.enums.BeneficiaryCategory;
import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.extraction.VisitExtraction;
import ai.swasthyavaani.domain.model.Actions;
import ai.swasthyavaani.domain.model.Beneficiary;
import ai.swasthyavaani.domain.model.Observations;
import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.SarvamException;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.TranscriptionRequest;
import ai.swasthyavaani.sarvam.model.TranscriptionResult;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VisitTranscriptionServiceTest {

  @Mock private SarvamClient sarvam;

  private VisitTranscriptionService service;

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
    var validator = new VisitExtractionValidator(JsonMapper.builder().findAndAddModules().build());
    var clock = Clock.fixed(Instant.parse("2026-08-28T09:14:00Z"), ZoneId.of("Asia/Kolkata"));
    var assembler = new VisitAssembler(validator, clock);
    service =
        new VisitTranscriptionService(
            sarvam, props, assembler, new ai.swasthyavaani.api.artifact.NoOpArtifactStore(), clock);
  }

  private TranscribeCommand command() {
    return new TranscribeCommand(
        "audio".getBytes(StandardCharsets.UTF_8),
        "visit.wav",
        "audio/wav",
        "bn-IN",
        "ASHA-WB-1",
        "dev-1",
        null,
        "Amtala",
        "WB",
        null);
  }

  @Test
  void assemblesDraftRecordFromTranscriptAndExtraction() {
    when(sarvam.transcribe(any(TranscriptionRequest.class)))
        .thenReturn(
            Mono.just(new TranscriptionResult("baby-r weight thik aache", "bn-IN", 0.9, "r-1")));
    when(sarvam.extractVisit(any(ExtractionRequest.class)))
        .thenReturn(
            Mono.just(
                new VisitExtraction(
                    new Beneficiary(
                        null, "Rekha Das", BeneficiaryCategory.PREGNANT_WOMAN, null, 24),
                    new Observations(52.5, null, null, 28, List.of("fever"), null),
                    new Actions(List.of("IFA tablets"), null, null),
                    List.of())));

    StepVerifier.create(service.transcribeAndExtract(command()))
        .assertNext(
            response -> {
              var visit = response.visit();
              org.junit.jupiter.api.Assertions.assertTrue(response.extractionValid());
              org.junit.jupiter.api.Assertions.assertEquals(
                  "baby-r weight thik aache", response.transcript());
              org.junit.jupiter.api.Assertions.assertEquals(
                  ConfirmationStatus.DRAFT, visit.confirmationStatus());
              org.junit.jupiter.api.Assertions.assertEquals(SyncStatus.PENDING, visit.syncStatus());
              org.junit.jupiter.api.Assertions.assertFalse(visit.createdOffline());
              org.junit.jupiter.api.Assertions.assertNotNull(visit.visitId());
              org.junit.jupiter.api.Assertions.assertEquals(
                  "Rekha Das", visit.beneficiary().name());
              org.junit.jupiter.api.Assertions.assertEquals(
                  "saaras:v3", visit.provenance().sttModel());
              org.junit.jupiter.api.Assertions.assertEquals(
                  "sarvam-m", visit.provenance().extractionModel());
              org.junit.jupiter.api.Assertions.assertEquals(
                  "bn-IN", visit.provenance().sourceLanguage());
              org.junit.jupiter.api.Assertions.assertEquals(1, visit.schemaVersion());
            })
        .verifyComplete();
  }

  @Test
  void returnsManualEntryDraftWhenExtractionFails() {
    when(sarvam.transcribe(any(TranscriptionRequest.class)))
        .thenReturn(Mono.just(new TranscriptionResult("garbled", "bn-IN", 0.5, "r-2")));
    when(sarvam.extractVisit(any(ExtractionRequest.class)))
        .thenReturn(Mono.error(new SarvamException("bad json", 422)));

    StepVerifier.create(service.transcribeAndExtract(command()))
        .assertNext(
            response -> {
              org.junit.jupiter.api.Assertions.assertFalse(response.extractionValid());
              org.junit.jupiter.api.Assertions.assertEquals("garbled", response.transcript());
              org.junit.jupiter.api.Assertions.assertEquals(
                  ConfirmationStatus.DRAFT, response.visit().confirmationStatus());
              org.junit.jupiter.api.Assertions.assertNull(response.visit().beneficiary());
              org.junit.jupiter.api.Assertions.assertFalse(response.validationMessages().isEmpty());
            })
        .verifyComplete();
  }
}
