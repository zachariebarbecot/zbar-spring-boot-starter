package fr.zbar.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Coexistence with zbar-logs: outgoing call logs and traceparent survive the status handlers. */
@ExtendWith(OutputCaptureExtension.class)
class RestClientLogsIT {

  private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
  private static final String TRACEPARENT = "00-" + TRACE_ID + "-b7ad6b7169203331-01";

  private RecordingServer server;

  @BeforeEach
  void startServer() throws IOException {
    server = new RecordingServer();
  }

  @AfterEach
  void stopServer() {
    server.close();
  }

  @Test
  void should_log_start_and_end_of_a_successful_call(CapturedOutput output) {
    try (ConfigurableApplicationContext context = run()) {
      RecordingServer.call(context, server.baseUrl() + "/ok");
    }

    assertStartThenEnd(output, "/ok", "200");
  }

  @Test
  void should_log_start_and_end_of_calls_answered_4xx_and_5xx(CapturedOutput output) {
    try (ConfigurableApplicationContext context = run()) {
      server.respondWith(404, "text/plain", "");
      assertThatThrownBy(() -> RecordingServer.call(context, server.baseUrl() + "/four"))
          .isInstanceOf(RestClientClientErrorException.class);
      server.respondWith(503, "text/plain", "");
      assertThatThrownBy(() -> RecordingServer.call(context, server.baseUrl() + "/five"))
          .isInstanceOf(RestClientServerErrorException.class);
    }

    assertThat(outgoingMessages(output))
        .hasSize(4)
        .satisfiesExactly(
            m -> assertThat(m).matches("outgoing start GET localhost:\\d+/four"),
            m ->
                assertThat(m)
                    .matches(
                        "outgoing end GET localhost:\\d+/four failed=RestClientClientErrorException .*"),
            m -> assertThat(m).matches("outgoing start GET localhost:\\d+/five"),
            m ->
                assertThat(m)
                    .matches(
                        "outgoing end GET localhost:\\d+/five failed=RestClientServerErrorException .*"));
  }

  @Test
  void should_forward_incoming_trace_id_on_a_successful_call() throws Exception {
    try (ConfigurableApplicationContext context = run()) {
      relay(context);
    }

    assertThat(server.lastHeaders().getFirst("traceparent"))
        .matches("00-" + TRACE_ID + "-[0-9a-f]{16}-01");
  }

  @Test
  void should_forward_incoming_trace_id_when_the_call_fails() throws Exception {
    server.respondWith(500, "text/plain", "");
    try (ConfigurableApplicationContext context = run()) {
      relay(context);
      assertThat(server.lastHeaders().getFirst("traceparent"))
          .matches("00-" + TRACE_ID + "-[0-9a-f]{16}-01");

      server.respondWith(404, "text/plain", "");
      relay(context);
      assertThat(server.lastHeaders().getFirst("traceparent"))
          .matches("00-" + TRACE_ID + "-[0-9a-f]{16}-01");
    }
  }

  private void assertStartThenEnd(CapturedOutput output, String path, String status) {
    assertThat(outgoingMessages(output))
        .hasSize(2)
        .satisfiesExactly(
            m -> assertThat(m).matches("outgoing start GET localhost:\\d+" + path),
            m ->
                assertThat(m)
                    .matches(
                        "outgoing end GET localhost:\\d+" + path + " status=" + status + " .*"));
  }

  private void relay(ConfigurableApplicationContext context) throws Exception {
    String port = context.getEnvironment().getProperty("local.server.port");
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(
                    "http://localhost:" + port + "/relay?target=" + server.baseUrl() + "/ping"))
            .header("traceparent", TRACEPARENT)
            .build();
    HttpClient.newHttpClient().send(request, BodyHandlers.discarding());
  }

  private static List<String> outgoingMessages(CapturedOutput output) {
    return output
        .getOut()
        .lines()
        .filter(line -> line.startsWith("{") && line.contains("\"message\":\"outgoing "))
        .map(line -> JsonMapper.shared().readTree(line))
        .map((JsonNode event) -> event.get("message").asString())
        .toList();
  }

  private ConfigurableApplicationContext run() {
    return new SpringApplicationBuilder(LogsApplication.class)
        .web(WebApplicationType.SERVLET)
        .properties("server.port=0", "logging.structured.format.console=logstash")
        .run();
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(RelayController.class)
  static class LogsApplication {}

  @RestController
  static class RelayController {

    private final RestClient client;

    RelayController(RestClient.Builder builder) {
      this.client = builder.build();
    }

    @GetMapping("/relay")
    void relay(@RequestParam String target) {
      client.get().uri(target).retrieve().toBodilessEntity();
    }
  }
}
