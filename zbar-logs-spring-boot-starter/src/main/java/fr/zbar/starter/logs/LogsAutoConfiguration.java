package fr.zbar.starter.logs;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@ConditionalOnProperty(prefix = "zbar.logs", name = "enabled", matchIfMissing = true)
@AutoConfiguration
@EnableConfigurationProperties(LogsProperties.class)
public class LogsAutoConfiguration {

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  @ConditionalOnWebApplication(type = Type.SERVLET)
  RequestLoggingObservationHandler requestLoggingObservationHandler(
      Tracer tracer, LogsProperties properties) {
    return new RequestLoggingObservationHandler(tracer, properties.excludePaths());
  }

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  OutgoingRequestLoggingObservationHandler outgoingRequestLoggingObservationHandler(Tracer tracer) {
    return new OutgoingRequestLoggingObservationHandler(tracer);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
  static class WebClientLogging {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    WebClientLoggingObservationHandler webClientLoggingObservationHandler(Tracer tracer) {
      return new WebClientLoggingObservationHandler(tracer);
    }
  }
}
