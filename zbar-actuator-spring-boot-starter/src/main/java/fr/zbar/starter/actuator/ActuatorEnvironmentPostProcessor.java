package fr.zbar.starter.actuator;

import java.util.HashMap;
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
    Map<String, Object> defaults = new HashMap<>();
    defaults.put("management.endpoints.web.exposure.include", "health,info,prometheus");
    defaults.put("server.shutdown", "graceful");
    defaults.put("spring.lifecycle.timeout-per-shutdown-phase", "30s");
    String applicationName = environment.getProperty("spring.application.name", "");
    if (!applicationName.isEmpty()) {
      defaults.put("management.metrics.tags.application", applicationName);
    }
    environment
        .getPropertySources()
        .addLast(new MapPropertySource("zbarActuatorDefaults", defaults));
  }
}
