package ai.swasthyavaani.sarvam.impl;

import ai.swasthyavaani.domain.extraction.VisitExtraction;
import ai.swasthyavaani.sarvam.SarvamClient;
import ai.swasthyavaani.sarvam.SarvamException;
import ai.swasthyavaani.sarvam.config.SarvamProperties;
import ai.swasthyavaani.sarvam.model.ExtractionRequest;
import ai.swasthyavaani.sarvam.model.SpeechRequest;
import ai.swasthyavaani.sarvam.model.SpeechResult;
import ai.swasthyavaani.sarvam.model.SttMode;
import ai.swasthyavaani.sarvam.model.TranscriptionRequest;
import ai.swasthyavaani.sarvam.model.TranscriptionResult;
import ai.swasthyavaani.sarvam.model.TranslationRequest;
import ai.swasthyavaani.sarvam.model.TranslationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * The single {@link SarvamClient} implementation. Every capability is a reactive call through one
 * {@link WebClient}, wrapped with the timeout + bounded-backoff-retry policy from {@code
 * sarvam-integration.md} §9 and mapped to {@link SarvamException} on failure.
 *
 * <p>Contracts here are the verified Aug-2026 shapes ({@code sarvam-integration.md}); items marked
 * "assumption — verify" in that doc should be re-checked against live docs before production use.
 */
@Component
public class SarvamWebClient implements SarvamClient {

  private static final Logger log = LoggerFactory.getLogger(SarvamWebClient.class);

  private final WebClient webClient;
  private final SarvamProperties props;
  private final ObjectMapper objectMapper;

  public SarvamWebClient(
      WebClient sarvamHttpClient, SarvamProperties props, ObjectMapper objectMapper) {
    this.webClient = sarvamHttpClient;
    this.props = props;
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<TranscriptionResult> transcribe(TranscriptionRequest request) {
    var mode = request.mode() != null ? request.mode() : SttMode.CODEMIX;
    var body = new MultipartBodyBuilder();
    body.part("file", audioResource(request), MediaType.parseMediaType(safeContentType(request)))
        .filename(request.filename() != null ? request.filename() : "audio");
    body.part("model", props.models().stt());
    body.part("mode", mode.wire());
    if (request.languageCode() != null) {
      body.part("language_code", request.languageCode());
    }

    return webClient
        .post()
        .uri("/speech-to-text")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(body.build()))
        .retrieve()
        .bodyToMono(JsonNode.class)
        .map(
            node ->
                new TranscriptionResult(
                    text(node, "transcript"),
                    text(node, "language_code"),
                    node.hasNonNull("language_probability")
                        ? node.get("language_probability").asDouble()
                        : null,
                    text(node, "request_id")))
        .transform(this::resilience);
  }

  @Override
  public Mono<VisitExtraction> extractVisit(ExtractionRequest request) {
    var payload =
        Map.of(
            "model",
            props.models().extraction(),
            "temperature",
            0.1,
            "top_p",
            1,
            "messages",
            List.of(
                Map.of("role", "system", "content", ExtractionPrompt.SYSTEM),
                Map.of("role", "user", "content", ExtractionPrompt.user(request))));

    return webClient
        .post()
        .uri("/v1/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(payload)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .flatMap(this::parseExtraction)
        .transform(this::resilience);
  }

  @Override
  public Mono<SpeechResult> synthesizeSpeech(SpeechRequest request) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("text", request.text());
    payload.put("target_language_code", request.targetLanguageCode());
    payload.put("model", props.models().tts());
    payload.put(
        "speaker", request.speaker() != null ? request.speaker() : props.tts().defaultSpeaker());
    payload.put("speech_sample_rate", props.tts().sampleRate());

    return webClient
        .post()
        .uri("/text-to-speech")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(payload)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .map(
            node -> {
              var joined = new StringBuilder();
              var audios = node.get("audios");
              if (audios != null && audios.isArray()) {
                audios.forEach(chunk -> joined.append(chunk.asText()));
              }
              byte[] decoded = Base64.getDecoder().decode(joined.toString());
              return new SpeechResult(decoded, text(node, "request_id"));
            })
        .transform(this::resilience);
  }

  @Override
  public Mono<TranslationResult> translate(TranslationRequest request) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("input", request.input());
    payload.put(
        "source_language_code",
        request.sourceLanguageCode() != null ? request.sourceLanguageCode() : "auto");
    payload.put("target_language_code", request.targetLanguageCode());
    payload.put("model", props.models().translate());

    return webClient
        .post()
        .uri("/translate")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(payload)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .map(
            node ->
                new TranslationResult(
                    node.hasNonNull("translated_text")
                        ? node.get("translated_text").asText()
                        : text(node, "output"),
                    text(node, "request_id")))
        .transform(this::resilience);
  }

  // --- helpers -------------------------------------------------------------

  private Mono<VisitExtraction> parseExtraction(JsonNode node) {
    try {
      var content = node.at("/choices/0/message/content").asText("");
      var json = stripFences(content);
      return Mono.just(objectMapper.readValue(json, VisitExtraction.class));
    } catch (Exception e) {
      // Fail loud: the caller keeps the visit as DRAFT and surfaces the raw transcript.
      return Mono.error(new SarvamException("Extraction returned non-conformant JSON", 422, e));
    }
  }

  /** Defensively strip ```json fences the model may add despite the JSON-only instruction. */
  private static String stripFences(String content) {
    var trimmed = content.strip();
    if (trimmed.startsWith("```")) {
      int firstNewline = trimmed.indexOf('\n');
      if (firstNewline > 0) {
        trimmed = trimmed.substring(firstNewline + 1);
      }
      if (trimmed.endsWith("```")) {
        trimmed = trimmed.substring(0, trimmed.length() - 3);
      }
    }
    return trimmed.strip();
  }

  private ByteArrayResource audioResource(TranscriptionRequest request) {
    return new ByteArrayResource(request.audio()) {
      @Override
      public String getFilename() {
        return request.filename() != null ? request.filename() : "audio";
      }
    };
  }

  private static String safeContentType(TranscriptionRequest request) {
    return request.contentType() != null
        ? request.contentType()
        : MediaType.APPLICATION_OCTET_STREAM_VALUE;
  }

  private static String text(JsonNode node, String field) {
    return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
  }

  /** Apply bounded retry-with-backoff on retryable failures, then map errors to SarvamException. */
  private <T> Mono<T> resilience(Mono<T> source) {
    return source
        .retryWhen(
            Retry.backoff(props.retry().maxAttempts(), props.retry().initialBackoff())
                .jitter(0.5)
                .filter(SarvamWebClient::isRetryable))
        .onErrorMap(SarvamWebClient::isNotAlreadySarvam, SarvamWebClient::toSarvamException);
  }

  private static boolean isRetryable(Throwable t) {
    if (t instanceof WebClientResponseException wcre) {
      int code = wcre.getStatusCode().value();
      return code == 429 || code == 503;
    }
    // network / timeout errors (no HTTP response) are retryable
    return !(t instanceof SarvamException);
  }

  private static boolean isNotAlreadySarvam(Throwable t) {
    return !(t instanceof SarvamException);
  }

  private static SarvamException toSarvamException(Throwable t) {
    if (t instanceof WebClientResponseException wcre) {
      log.warn("Sarvam call failed: HTTP {}", wcre.getStatusCode().value());
      return new SarvamException(
          "Sarvam call failed with HTTP " + wcre.getStatusCode().value(),
          wcre.getStatusCode().value(),
          t);
    }
    log.warn("Sarvam call failed (transport): {}", t.getClass().getSimpleName());
    return new SarvamException("Sarvam call failed: " + t.getClass().getSimpleName(), 0, t);
  }

  /**
   * Extraction prompt lives with the client, never inline in a controller ({@code CLAUDE.md} §6).
   */
  private static final class ExtractionPrompt {
    private static final String SYSTEM =
        """
        You convert an ASHA/ANM health worker's dictated home-visit transcript into a structured
        record. The speech is code-mixed Bengali + English (e.g. "baby-r weight thik aache, kintu
        fever holo kal theke") — understand both languages and Romanised Bengali.

        Output STRICT JSON ONLY — no prose, no markdown fences. Exact shape:
        {"beneficiary":{"name","category","gender","ageYears"},
         "observations":{"weightKg","temperatureC","bloodPressure":{"systolic","diastolic"},
                         "gestationWeeks","reportedSymptoms":[],"notes"},
         "actions":{"medicinesHandedOver":[],"referral":{"referred","facility","urgency","reason"},
                    "nextVisitDate"},
         "warnings":[]}

        Controlled vocabulary (use these exact values or null):
        - category: PREGNANT_WOMAN | LACTATING_MOTHER | INFANT | CHILD_UNDER_5 | ADULT | OTHER
        - gender:   FEMALE | MALE | OTHER
        - referral.urgency: ROUTINE | URGENT | EMERGENCY

        Rules:
        1. Any field not clearly stated in the transcript -> null (or omit arrays). NEVER fabricate.
        2. If a value is inferred rather than explicit, still fill it and add a human-readable note
           to "warnings" (e.g. "temperatureC inferred from 'thora jor' — confirm with worker").
        3. Numbers are numbers, not strings; dates are ISO-8601 (YYYY-MM-DD); weight in kg, temp °C.
        4. No diagnosis and no medication advice — only medicines the worker says were handed over.
        5. reportedSymptoms are the worker's own words translated to English, not a clinical judgement.
        """;

    private static String user(ExtractionRequest request) {
      var sb = new StringBuilder();
      if (request.context() != null && !request.context().isBlank()) {
        sb.append("Context: ").append(request.context()).append("\n\n");
      }
      sb.append("Transcript:\n").append(request.transcript());
      return sb.toString();
    }

    private ExtractionPrompt() {}
  }
}
