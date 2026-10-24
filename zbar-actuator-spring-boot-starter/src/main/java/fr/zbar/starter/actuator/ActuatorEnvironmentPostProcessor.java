package fr.zbar.starter.actuator;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@Order(Ordered.LOWEST_PRECEDENCE)
class ActuatorEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if ("false".equalsIgnoreCase(environment.getProperty("zbar.actuator.enabled"))) {
      return;
    }
    environment
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "zbarActuatorDefaults",
                Map.of("management.endpoints.web.exposure.include", "health,info,prometheus")));
  }
}
