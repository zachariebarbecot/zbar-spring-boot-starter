package fr.zbar.starter.restclient;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@Order(Ordered.LOWEST_PRECEDENCE)
class RestClientEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if ("false".equalsIgnoreCase(environment.getProperty("zbar.restclient.enabled"))) {
      return;
    }
    environment
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "zbarRestClientDefaults",
                Map.of(
                    "spring.http.clients.connect-timeout", "2s",
                    "spring.http.clients.read-timeout", "10s")));
  }
}
