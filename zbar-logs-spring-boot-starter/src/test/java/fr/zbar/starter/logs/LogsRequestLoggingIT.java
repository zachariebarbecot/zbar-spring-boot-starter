package fr.zbar.starter.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class LogsRequestLoggingIT {

  @Test
  void should_log_start_then_end_with_status_and_duration_and_same_trace_id(CapturedOutput output) {
    try (ConfigurableApplicationContext context = run()) {
      call(context, "/x");
    }

    List<JsonNode> events = requestEvents(output);
    assertThat(events).hasSize(2);
    JsonNode start = events.get(0);
    JsonNode end = events.get(1);
    assertThat(start.get("level").asString()).isEqualTo("INFO");
    assertThat(start.get("message").asString()).isEqualTo("request start GET /x");
    assertThat(end.get("level").asString()).isEqualTo("INFO");
    assertThat(end.get("message").asString())
        .matches("request end GET /x status=200 duration=\\d+ms");
    assertThat(start.get("traceId").asString()).matches("[0-9a-f]{32}");
    assertThat(end.get("traceId").asString()).isEqualTo(start.get("traceId").asString());
  }

  @Test
  void should_log_end_with_error_status_and_duration_when_controller_throws(CapturedOutput output) {
    HttpStatusCode status;
    try (ConfigurableApplicationContext context = run()) {
      status = callExpectingError(context, "/boom");
    }

    assertThat(status.value()).isEqualTo(500);
    List<JsonNode> events = requestEvents(output);
    assertThat(events).hasSize(2);
    assertThat(events.get(1).get("message").asString())
        .matches("request end GET /boom status=500 duration=\\d+ms");
  }

  @Test
  void should_log_only_the_path_and_never_query_or_headers(CapturedOutput output) {
    try (ConfigurableApplicationContext context = run()) {
      String port = context.getEnvironment().getProperty("local.server.port");
      RestClient.create("http://localhost:" + port)
          .get()
          .uri("/x?token=secret-xyz")
          .header("Authorization", "secret-xyz")
          .retrieve()
          .toBodilessEntity();
    }

    assertThat(requestEvents(output)).hasSize(2);
    assertThat(output.getOut()).doesNotContain("secret-xyz");
  }

  @Test
  void should_not_log_default_excluded_paths(CapturedOutput output) {
    try (ConfigurableApplicationContext context = run()) {
      call(context, "/actuator/health");
      call(context, "/v3/api-docs");
    }

    assertThat(requestEvents(output)).isEmpty();
  }

  @Test
  void should_replace_default_exclusions_with_configured_paths(CapturedOutput output) {
    try (ConfigurableApplicationContext context = run("zbar.logs.exclude-paths=/internal/**")) {
      call(context, "/internal/ping");
      call(context, "/x");
    }

    assertThat(requestEvents(output))
        .extracting(event -> event.get("message").asString())
        .allMatch(message -> message.contains("GET /x"))
        .hasSize(2);
  }

  private static List<JsonNode> requestEvents(CapturedOutput output) {
    return output
        .getOut()
        .lines()
        .filter(line -> line.startsWith("{") && line.contains("\"message\":\"request "))
        .map(line -> JsonMapper.shared().readTree(line))
        .toList();
  }

  private ConfigurableApplicationContext run(String... properties) {
    return new SpringApplicationBuilder(App.class)
        .web(WebApplicationType.SERVLET)
        .properties("server.port=0", "logging.structured.format.console=logstash")
        .properties(properties)
        .run();
  }

  private void call(ConfigurableApplicationContext context, String path) {
    String port = context.getEnvironment().getProperty("local.server.port");
    RestClient.create("http://localhost:" + port).get().uri(path).retrieve().toBodilessEntity();
  }

  private HttpStatusCode callExpectingError(ConfigurableApplicationContext context, String path) {
    String port = context.getEnvironment().getProperty("local.server.port");
    return RestClient.create("http://localhost:" + port)
        .get()
        .uri(path)
        .exchange((request, response) -> response.getStatusCode());
  }

  @Configuration
  @EnableAutoConfiguration
  static class App {

    @Bean
    PingController pingController() {
      return new PingController();
    }
  }

  @RestController
  static class PingController {

    @GetMapping({"/actuator/health", "/v3/api-docs", "/internal/ping"})
    String excluded() {
      return "ok";
    }

    @GetMapping("/boom")
    String boom() {
      throw new IllegalStateException("boom");
    }

    @GetMapping("/x")
    String x() {
      return "ok";
    }
  }
}
