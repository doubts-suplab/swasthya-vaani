package ai.swasthyavaani.api.sync;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the scheduled SQS reconcile worker — only under the {@code aws} profile. */
@Configuration
@Profile("aws")
@EnableScheduling
public class SchedulingConfig {}
