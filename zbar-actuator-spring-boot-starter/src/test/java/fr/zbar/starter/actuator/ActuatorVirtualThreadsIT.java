package fr.zbar.starter.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ActuatorVirtualThreadsIT {

  @Test
  void should_enable_virtual_threads_property_by_default() {
    try (ConfigurableApplicationContext context = run()) {
      assertThat(context.getEnvironment().getProperty("spring.threads.virtual.enabled"))
          .isEqualTo("true");
    }
  }

  @Test
  void should_handle_requests_on_virtual_thread_by_default() throws Exception {
    try (ConfigurableApplicationContext context = run()) {
      assertThat(isVirtualThread(context)).isTrue();
    }
  }

  @Test
  void should_let_application_disable_virtual_threads() throws Exception {
    try (ConfigurableApplicationContext context = run("--spring.threads.virtual.enabled=false")) {
      assertThat(isVirtualThread(context)).isFalse();
    }
  }

  @Test
  void should_not_enable_virtual_threads_when_disabled() throws Exception {
    try (ConfigurableApplicationContext context = run("--zbar.actuator.enabled=false")) {
      assertThat(isVirtualThread(context)).isFalse();
      assertThat(context.getEnvironment().getProperty("spring.threads.virtual.enabled")).isNull();
    }
  }

  private ConfigurableApplicationContext run(String... args) {
    return new SpringApplicationBuilder(WebApplication.class).properties("server.port=0").run(args);
  }

  private boolean isVirtualThread(ConfigurableApplicationContext context) throws Exception {
    String port = context.getEnvironment().getProperty("local.server.port");
    String body =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/thread")).build(),
                HttpResponse.BodyHandlers.ofString())
            .body();
    return Boolean.parseBoolean(body);
  }

  @Configuration
  @EnableAutoConfiguration
  static class WebApplication {

    @RestController
    static class ThreadController {

      @GetMapping("/thread")
      String thread() {
        return String.valueOf(Thread.currentThread().isVirtual());
      }
    }
  }
}
