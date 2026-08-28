package ai.swasthyavaani.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * SwasthyaVaani API — reactive (WebFlux) orchestration service. Scans {@code ai.swasthyavaani} so
 * the {@code sarvam} and {@code domain} modules are wired in, and picks up
 * {@code @ConfigurationProperties} records (e.g. {@code SarvamProperties}).
 */
@SpringBootApplication(scanBasePackages = "ai.swasthyavaani")
@ConfigurationPropertiesScan("ai.swasthyavaani")
public class SwasthyaVaaniApplication {

  public static void main(String[] args) {
    SpringApplication.run(SwasthyaVaaniApplication.class, args);
  }
}
