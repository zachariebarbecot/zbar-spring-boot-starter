package fr.zbar.starter.errors;

import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ConditionalOnProperty(prefix = "zbar.errors", name = "enabled", matchIfMissing = true)
@AutoConfiguration(
    beforeName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@EnableConfigurationProperties(ErrorsProperties.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(ResponseEntityExceptionHandler.class)
public class ErrorsAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
  ErrorsExceptionHandler errorsExceptionHandler(TraceIdResolver traceIds) {
    return new ErrorsExceptionHandler(traceIds);
  }

  @Bean
  @ConditionalOnMissingBean
  TraceIdResolver noTraceIdResolver() {
    return () -> null;
  }

  /** Isolated so that the starter loads without Micrometer Tracing on the classpath. */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(Tracer.class)
  static class TracingConfiguration {

    @Bean
    TraceIdResolver tracerTraceIdResolver(ObjectProvider<Tracer> tracer) {
      return new TracerTraceIdResolver(tracer);
    }
  }
}
