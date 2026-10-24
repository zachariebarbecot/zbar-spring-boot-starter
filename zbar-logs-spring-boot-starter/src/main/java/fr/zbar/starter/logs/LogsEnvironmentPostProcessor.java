package fr.zbar.starter.logs;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

@Order(Ordered.LOWEST_PRECEDENCE)
class LogsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if ("false".equalsIgnoreCase(environment.getProperty("zbar.logs.enabled"))
        || !environment.acceptsProfiles(Profiles.of("json"))) {
      return;
    }
    environment
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "zbarLogsJson", Map.of("logging.structured.format.console", "logstash")));
  }
}
