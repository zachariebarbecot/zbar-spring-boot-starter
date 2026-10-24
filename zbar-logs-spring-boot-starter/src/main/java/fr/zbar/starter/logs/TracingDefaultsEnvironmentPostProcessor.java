package fr.zbar.starter.logs;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Samples every trace by default, so that the outgoing {@code traceparent} is sampled. */
@Order(Ordered.LOWEST_PRECEDENCE)
class TracingDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if ("false".equalsIgnoreCase(environment.getProperty("zbar.logs.enabled"))) {
      return;
    }
    environment
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "zbarLogsTracingDefaults",
                Map.of("management.tracing.sampling.probability", "1.0")));
  }
}
