package ai.swasthyavaani.sarvam.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Builds the reactive {@link WebClient} used by the single {@code SarvamClient} implementation.
 * Applies the base URL, the {@code api-subscription-key} auth header, connect/read timeouts, and a
 * larger in-memory buffer for audio payloads. Config comes entirely from {@link SarvamProperties}.
 */
@Configuration
@EnableConfigurationProperties(SarvamProperties.class)
public class SarvamClientConfig {

  private static final int MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024; // 16 MiB (audio)

  @Bean
  WebClient sarvamHttpClient(SarvamProperties props, WebClient.Builder builder) {
    HttpClient httpClient =
        HttpClient.create()
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.timeout().connect().toMillis())
            .responseTimeout(props.timeout().read());

    return builder
        .baseUrl(props.baseUrl())
        .defaultHeader("api-subscription-key", props.apiKey() == null ? "" : props.apiKey())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
        .build();
  }
}
