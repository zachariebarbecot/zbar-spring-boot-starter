package fr.zbar.starter.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class ActuatorMetricsTagIT {

  @Test
  void should_tag_metrics_with_application_name() throws Exception {
    try (ConfigurableApplicationContext context = run("--spring.application.name=demo")) {
      assertThat(applicationTag(context)).isEqualTo("demo");
      assertThat(prometheus(context)).contains("application=\"demo\"");
    }
  }

  @Test
  void should_not_tag_metrics_when_application_name_is_absent() {
    try (ConfigurableApplicationContext context = run()) {
      assertThat(applicationTag(context)).isNull();
    }
  }

  @Test
  void should_not_tag_metrics_when_application_name_is_empty() {
    try (ConfigurableApplicationContext context = run("--spring.application.name=")) {
      assertThat(applicationTag(context)).isNull();
    }
  }

  @Test
  void should_let_application_tag_win() {
    try (ConfigurableApplicationContext context =
        run("--spring.application.name=demo", "--management.metrics.tags.application=autre")) {
      assertThat(applicationTag(context)).isEqualTo("autre");
    }
  }

  @Test
  void should_not_tag_metrics_when_disabled() {
    try (ConfigurableApplicationContext context =
        run("--spring.application.name=demo", "--zbar.actuator.enabled=false")) {
      assertThat(applicationTag(context)).isNull();
    }
  }

  private String applicationTag(ConfigurableApplicationContext context) {
    return context.getBean(MeterRegistry.class).counter("zbar.test").getId().getTag("application");
  }

  private String prometheus(ConfigurableApplicationContext context) throws Exception {
    String port = context.getEnvironment().getProperty("local.server.port");
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/prometheus"))
                .build(),
            HttpResponse.BodyHandlers.ofString())
        .body();
  }

  private ConfigurableApplicationContext run(String... args) {
    return new SpringApplicationBuilder(PlainApplication.class)
        .properties("server.port=0")
        .run(args);
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
