package fr.zbar.starter.logs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

@Order(Ordered.LOWEST_PRECEDENCE)
class LogsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String DEFAULT_GIT = "classpath:git.properties";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if ("false".equalsIgnoreCase(environment.getProperty("zbar.logs.enabled"))
        || !environment.acceptsProfiles(Profiles.of("json"))) {
      return;
    }
    Map<String, Object> properties = new HashMap<>();
    properties.put("logging.structured.format.console", "logstash");
    addField(properties, "application", environment.getProperty("spring.application.name"));
    addField(properties, "version", version(environment));
    addField(properties, "environment", deployedEnvironment(environment));
    environment.getPropertySources().addLast(new MapPropertySource("zbarLogsJson", properties));
  }

  private static void addField(Map<String, Object> properties, String name, String value) {
    if (value != null && !value.isBlank()) {
      properties.put("logging.structured.json.add." + name, value);
    }
  }

  private static String version(ConfigurableEnvironment environment) {
    return gitCommit(environment);
  }

  private static String gitCommit(ConfigurableEnvironment environment) {
    Resource git =
        new DefaultResourceLoader()
            .getResource(environment.getProperty("spring.info.git.location", DEFAULT_GIT));
    if (!git.exists()) {
      return null;
    }
    try {
      return PropertiesLoaderUtils.loadProperties(git).getProperty("git.commit.id.abbrev");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String deployedEnvironment(ConfigurableEnvironment environment) {
    String configured = environment.getProperty("zbar.logs.environment");
    if (configured != null) {
      return configured;
    }
    return null;
  }
}
