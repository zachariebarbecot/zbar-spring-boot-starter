package fr.zbar.starter.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ActuatorGracefulShutdownIT {

  @Test
  void should_shutdown_gracefully_with_30s_timeout_by_default() {
    try (ConfigurableApplicationContext context = run()) {
      assertThat(context.getEnvironment().getProperty("server.shutdown")).isEqualTo("graceful");
      assertThat(
              context.getEnvironment().getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
          .isEqualTo("30s");
    }
  }

  @Test
  void should_complete_in_flight_request_when_context_closes() throws Exception {
    SlowController.started = new CountDownLatch(1);
    ConfigurableApplicationContext context = run();
    String port = context.getEnvironment().getProperty("local.server.port");
    CompletableFuture<HttpResponse<String>> response =
        HttpClient.newHttpClient()
            .sendAsync(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/slow")).build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(SlowController.started.await(5, TimeUnit.SECONDS)).isTrue();

    context.close();

    HttpResponse<String> result = response.get(10, TimeUnit.SECONDS);
    assertThat(result.statusCode()).isEqualTo(200);
    assertThat(result.body()).isEqualTo("done");
  }

  @Test
  void should_let_application_override_shutdown_mode() {
    try (ConfigurableApplicationContext context = run("--server.shutdown=immediate")) {
      assertThat(context.getEnvironment().getProperty("server.shutdown")).isEqualTo("immediate");
    }
  }

  @Test
  void should_let_application_override_shutdown_timeout() {
    try (ConfigurableApplicationContext context =
        run("--spring.lifecycle.timeout-per-shutdown-phase=10s")) {
      assertThat(
              context.getEnvironment().getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
          .isEqualTo("10s");
    }
  }

  @Test
  void should_not_provide_shutdown_properties_when_disabled() {
    try (ConfigurableApplicationContext context = run("--zbar.actuator.enabled=false")) {
      assertThat(context.getEnvironment().getProperty("server.shutdown")).isNull();
      assertThat(
              context.getEnvironment().getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
          .isNull();
    }
  }

  private ConfigurableApplicationContext run(String... args) {
    return new SpringApplicationBuilder(PlainApplication.class)
        .properties("server.port=0")
        .run(args);
  }

  @RestController
  static class SlowController {
    static volatile CountDownLatch started;

    @GetMapping("/slow")
    String slow() throws InterruptedException {
      started.countDown();
      Thread.sleep(1500);
      return "done";
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(SlowController.class)
  static class PlainApplication {}
}
