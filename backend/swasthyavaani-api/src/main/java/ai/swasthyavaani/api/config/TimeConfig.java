package ai.swasthyavaani.api.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides an injectable {@link Clock} (IST) so timestamp generation is deterministic in tests. */
@Configuration
public class TimeConfig {

  /** India Standard Time — visit timestamps are recorded in the worker's local zone. */
  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("Asia/Kolkata"));
  }
}
