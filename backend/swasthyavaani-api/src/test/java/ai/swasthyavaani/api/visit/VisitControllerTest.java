package ai.swasthyavaani.api.visit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ai.swasthyavaani.api.error.GlobalErrorHandler;
import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.enums.VisitType;
import ai.swasthyavaani.domain.model.VisitRecord;
import ai.swasthyavaani.sarvam.SarvamException;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = VisitController.class)
@Import(GlobalErrorHandler.class)
class VisitControllerTest {

  @Autowired private WebTestClient webTestClient;

  @MockitoBean private VisitTranscriptionService service;

  private static BodyInserters.MultipartInserter audioBody() {
    var mb = new MultipartBodyBuilder();
    mb.part(
            "file",
            new ByteArrayResource("fake-audio".getBytes()) {
              @Override
              public String getFilename() {
                return "visit.wav";
              }
            })
        .contentType(MediaType.parseMediaType("audio/wav"));
    mb.part("workerId", "ASHA-WB-1");
    return BodyInserters.fromMultipartData(mb.build());
  }

  @Test
  void returnsDraftRecord() {
    var now = OffsetDateTime.parse("2026-08-28T09:14:00+05:30");
    var record =
        new VisitRecord(
            "vid-1",
            1,
            "ASHA-WB-1",
            "dev",
            VisitType.GENERAL,
            now,
            null,
            null,
            null,
            null,
            null,
            ConfirmationStatus.DRAFT,
            SyncStatus.PENDING,
            false,
            now,
            now,
            null);
    when(service.transcribeAndExtract(any()))
        .thenReturn(Mono.just(new VisitDraftResponse(record, "transcript text", true, List.of())));

    webTestClient
        .post()
        .uri("/api/v1/visits/transcribe")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(audioBody())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.visit.visitId")
        .isEqualTo("vid-1")
        .jsonPath("$.visit.confirmationStatus")
        .isEqualTo("DRAFT")
        .jsonPath("$.transcript")
        .isEqualTo("transcript text")
        .jsonPath("$.extractionValid")
        .isEqualTo(true);
  }

  @Test
  void mapsSarvamFailureToBadGateway() {
    when(service.transcribeAndExtract(any()))
        .thenReturn(Mono.error(new SarvamException("upstream down", 503)));

    webTestClient
        .post()
        .uri("/api/v1/visits/transcribe")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(audioBody())
        .exchange()
        .expectStatus()
        .isEqualTo(502);
  }
}
