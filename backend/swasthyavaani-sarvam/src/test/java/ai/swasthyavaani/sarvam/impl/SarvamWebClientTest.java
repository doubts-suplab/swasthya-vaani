package ai.swasthyavaani.sarvam.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.SpeechRequest;
import ai.swasthyavaani.sarvam.model.TranscriptionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

/** Verifies the single Sarvam client parses each contract correctly, against a mock HTTP server. */
class SarvamWebClientTest {

  private MockWebServer server;
  private SarvamWebClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    var props =
        new SarvamProperties(
            "test-key",
            server.url("/").toString(),
            "wss://example/ws",
            new SarvamProperties.Languages("bn-IN", List.of("bn-IN", "en-IN")),
            new SarvamProperties.Models(
                "saaras:v3", "saaras:v3-realtime", "sarvam-m", "bulbul:v3", "sarvam-translate:v1"),
            new SarvamProperties.Tts("shubh", 22050),
            new SarvamProperties.Timeouts(Duration.ofSeconds(3), Duration.ofSeconds(30)),
            new SarvamProperties.Retry(2, Duration.ofMillis(10)));

    ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    WebClient webClient =
        WebClient.builder()
            .baseUrl(props.baseUrl())
            .defaultHeader("api-subscription-key", props.apiKey())
            .build();
    client = new SarvamWebClient(webClient, props, mapper);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void transcribeParsesResponse() {
    server.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {"request_id":"r-1","transcript":"baby-r weight thik aache",
                 "language_code":"bn-IN","language_probability":0.92}
                """));

    var request =
        new TranscriptionRequest(
            "fake-audio".getBytes(StandardCharsets.UTF_8), "visit.wav", "audio/wav", "bn-IN", null);

    StepVerifier.create(client.transcribe(request))
        .assertNext(
            result -> {
              assertEquals("baby-r weight thik aache", result.transcript());
              assertEquals("bn-IN", result.languageCode());
              assertEquals(0.92, result.languageProbability());
              assertEquals("r-1", result.requestId());
            })
        .verifyComplete();
  }

  @Test
  void extractVisitParsesJsonFromChatCompletion() throws Exception {
    // Build a valid chat-completion body whose content is the extraction JSON wrapped in
    // ```json fences — exercising the defensive fence-stripping in the client.
    ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    var extractionPayload =
        Map.of(
            "beneficiary", Map.of("name", "Rekha Das", "ageYears", 24),
            "observations", Map.of("weightKg", 52.5, "reportedSymptoms", List.of("fever")),
            "actions", Map.of("medicinesHandedOver", List.of("IFA tablets")),
            "warnings", List.of());
    String content = "```json\n" + mapper.writeValueAsString(extractionPayload) + "\n```";
    String body =
        mapper.writeValueAsString(
            Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));

    server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(body));

    StepVerifier.create(client.extractVisit(new ExtractionRequest("some transcript", null)))
        .assertNext(
            extraction -> {
              assertNotNull(extraction.beneficiary());
              assertEquals("Rekha Das", extraction.beneficiary().name());
              assertEquals(52.5, extraction.observations().weightKg());
              assertEquals(List.of("IFA tablets"), extraction.actions().medicinesHandedOver());
            })
        .verifyComplete();
  }

  @Test
  void synthesizeSpeechDecodesBase64() {
    byte[] raw = "wav-bytes".getBytes(StandardCharsets.UTF_8);
    String b64 = Base64.getEncoder().encodeToString(raw);
    server.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"request_id\":\"r-2\",\"audios\":[\"" + b64 + "\"]}"));

    StepVerifier.create(client.synthesizeSpeech(new SpeechRequest("ওজন ঠিক আছে", "bn-IN", null)))
        .assertNext(result -> assertArrayEquals(raw, result.audio()))
        .verifyComplete();
  }
}
