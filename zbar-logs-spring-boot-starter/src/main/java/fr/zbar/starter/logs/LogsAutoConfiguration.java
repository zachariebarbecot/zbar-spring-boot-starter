package fr.zbar.starter.logs;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@ConditionalOnProperty(prefix = "zbar.logs", name = "enabled", matchIfMissing = true)
@AutoConfiguration
@EnableConfigurationProperties(LogsProperties.class)
public class LogsAutoConfiguration {

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  RequestLoggingObservationHandler requestLoggingObservationHandler(
      Tracer tracer, LogsProperties properties) {
    return new RequestLoggingObservationHandler(tracer, properties.excludePaths());
  }
}
