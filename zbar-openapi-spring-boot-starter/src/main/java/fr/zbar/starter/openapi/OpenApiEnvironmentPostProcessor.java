package fr.zbar.starter.openapi;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@Order(Ordered.LOWEST_PRECEDENCE)
class OpenApiEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (!"false".equalsIgnoreCase(environment.getProperty("zbar.openapi.enabled"))) {
      return;
    }
    environment
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "zbarOpenApiDisabled",
                Map.of(
                    "springdoc.api-docs.enabled", "false",
                    "springdoc.swagger-ui.enabled", "false")));
  }
}
