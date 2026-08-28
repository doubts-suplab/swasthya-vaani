package ai.swasthyavaani.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Full-context smoke test: proves the whole application wires up — WebFlux, Actuator, the {@code
 * SarvamClient} bean and its config, and the {@code SarvamProperties} binding — with only an
 * example (blank) API key, i.e. without any live Sarvam call.
 */
@SpringBootTest
@TestPropertySource(properties = "sarvam.api-key=")
class SwasthyaVaaniApplicationTests {

  @Test
  void contextLoads() {
    // Context startup is the assertion; failure to wire any bean fails the test.
  }
}
