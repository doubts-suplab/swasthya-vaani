package ai.swasthyavaani.api.validation;

import ai.swasthyavaani.domain.extraction.VisitExtraction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Validates an extracted {@link VisitExtraction} against the JSON Schema in {@code data-model.md}
 * §5 before it is trusted/persisted. The schema is compiled once at startup.
 */
@Component
public class VisitExtractionValidator {

  private static final String SCHEMA_PATH = "schema/visit-extraction.schema.json";

  private final ObjectMapper objectMapper;
  private final JsonSchema schema;

  public VisitExtractionValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    try (InputStream in = new ClassPathResource(SCHEMA_PATH).getInputStream()) {
      this.schema = factory.getSchema(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to load " + SCHEMA_PATH, e);
    }
  }

  /**
   * Validate an extraction. Never throws for content problems — returns a {@link ValidationResult}.
   */
  public ValidationResult validate(VisitExtraction extraction) {
    JsonNode node = objectMapper.valueToTree(extraction);
    Set<ValidationMessage> errors = schema.validate(node);
    if (errors.isEmpty()) {
      return ValidationResult.ok();
    }
    List<String> messages =
        errors.stream()
            .map(ValidationMessage::getMessage)
            .sorted(Comparator.naturalOrder())
            .toList();
    return ValidationResult.invalid(messages);
  }
}
