package fr.zbar.starter.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class LogsOutgoingLoggingIT {

  private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";

  private HttpServer downstream;
  private String downstreamUrl;
  private final AtomicReference<String> receivedTraceparent = new AtomicReference<>();

  @BeforeEach
  void startDownstream() throws IOException {
    downstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    downstream.createContext(
        "/p",
        exchange -> {
          receivedTraceparent.set(exchange.getRequestHeaders().getFirst("traceparent"));
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    downstream.createContext(
        "/slow",
        exchange -> {
          try {
            Thread.sleep(1500);
            exchange.sendResponseHeaders(200, -1);
          } catch (InterruptedException | IOException e) {
            // client gave up
          } finally {
            exchange.close();
          }
        });
    downstream.start();
    downstreamUrl = "http://localhost:" + downstream.getAddress().getPort();
  }

  @AfterEach
  void stopDownstream() {
    downstream.stop(0);
  }

  @Test
  void should_log_start_then_end_of_a_rest_client_call(CapturedOutput output) {
    callWith(
        (context, url) ->
            context
                .getBean(RestClient.Builder.class)
                .build()
                .get()
                .uri(url)
                .retrieve()
                .toBodilessEntity());

    assertStartThenEnd(output);
  }

  @Test
  void should_log_start_then_end_of_a_rest_template_call(CapturedOutput output) {
    callWith(
        (context, url) ->
            context.getBean(RestTemplateBuilder.class).build().getForEntity(url, Void.class));

    assertStartThenEnd(output);
  }

  @Test
  void should_forward_incoming_trace_id_to_the_downstream_service() {
    try (ConfigurableApplicationContext context = run()) {
      relay(context, "00-" + TRACE_ID + "-b7ad6b7169203331-01");
    }

    assertThat(receivedTraceparent.get()).matches("00-" + TRACE_ID + "-[0-9a-f]{16}-01");
  }

  @Test
  void should_sample_and_forward_a_new_trace_when_no_traceparent_comes_in() {
    try (ConfigurableApplicationContext context = run()) {
      relay(context, null);
    }

    assertThat(receivedTraceparent.get()).matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[13]");
  }

  @Test
  void should_not_force_sampling_when_starter_is_disabled() {
    try (ConfigurableApplicationContext context = run("zbar.logs.enabled=false")) {
      assertThat(context.getEnvironment().getProperty("management.tracing.sampling.probability"))
          .isNull();
    }
  }

  private void relay(ConfigurableApplicationContext context, String traceparent) {
    String port = context.getEnvironment().getProperty("local.server.port");
    RestClient.RequestHeadersSpec<?> request =
        RestClient.create("http://localhost:" + port)
            .get()
            .uri("/relay?url={url}", downstreamUrl + "/p");
    if (traceparent != null) {
      request = request.header("traceparent", traceparent);
    }
    request.retrieve().toBodilessEntity();
  }

  @Test
  void should_log_failure_and_duration_and_propagate_exception_when_rest_client_times_out(
      CapturedOutput output) {
    assertTimeoutFailureLogged(
        output,
        (context, url) ->
            context
                .getBean(RestClient.Builder.class)
                .build()
                .get()
                .uri(url)
                .retrieve()
                .toBodilessEntity());
  }

  @Test
  void should_log_failure_and_duration_and_propagate_exception_when_rest_template_times_out(
      CapturedOutput output) {
    assertTimeoutFailureLogged(
        output,
        (context, url) ->
            context.getBean(RestTemplateBuilder.class).build().getForEntity(url, Void.class));
  }

  @Test
  void should_never_log_the_query_of_outgoing_calls(CapturedOutput output) {
    String url = downstreamUrl + "/p?apikey=secret-xyz";
    try (ConfigurableApplicationContext context = run()) {
      context
          .getBean(RestClient.Builder.class)
          .build()
          .get()
          .uri(url)
          .retrieve()
          .toBodilessEntity();
      context.getBean(RestTemplateBuilder.class).build().getForEntity(url, Void.class);
    }

    assertThat(outgoingEvents(output)).hasSize(4);
    assertThat(output.getOut()).doesNotContain("secret-xyz");
  }

  @Test
  void should_log_no_outgoing_call_when_starter_is_disabled(CapturedOutput output) {
    callWith(
        (context, url) ->
            context
                .getBean(RestClient.Builder.class)
                .build()
                .get()
                .uri(url)
                .retrieve()
                .toBodilessEntity(),
        "zbar.logs.enabled=false");

    assertThat(outgoingEvents(output)).isEmpty();
  }

  private void assertTimeoutFailureLogged(
      CapturedOutput output, BiConsumer<ConfigurableApplicationContext, String> call) {
    try (ConfigurableApplicationContext context = run("spring.http.clients.read-timeout=300ms")) {
      assertThatThrownBy(() -> call.accept(context, downstreamUrl + "/slow"))
          .isInstanceOf(RuntimeException.class);
    }

    List<JsonNode> events = outgoingEvents(output);
    assertThat(events).hasSize(2);
    assertThat(events.get(1).get("message").asString())
        .matches("outgoing end GET localhost:\\d+/slow failed=\\w*Exception duration=\\d+ms");
  }

  private void assertStartThenEnd(CapturedOutput output) {
    List<JsonNode> events = outgoingEvents(output);
    assertThat(events).hasSize(2);
    assertThat(events.get(0).get("level").asString()).isEqualTo("INFO");
    assertThat(events.get(0).get("message").asString())
        .matches("outgoing start GET localhost:\\d+/p");
    assertThat(events.get(1).get("level").asString()).isEqualTo("INFO");
    assertThat(events.get(1).get("message").asString())
        .matches("outgoing end GET localhost:\\d+/p status=200 duration=\\d+ms");
  }

  private void callWith(
      BiConsumer<ConfigurableApplicationContext, String> call, String... properties) {
    try (ConfigurableApplicationContext context = run(properties)) {
      call.accept(context, downstreamUrl + "/p");
    }
  }

  private static List<JsonNode> outgoingEvents(CapturedOutput output) {
    return output
        .getOut()
        .lines()
        .filter(line -> line.startsWith("{") && line.contains("\"message\":\"outgoing "))
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

  @Configuration
  @EnableAutoConfiguration
  static class App {

    @Bean
    RelayController relayController(RestClient.Builder builder) {
      return new RelayController(builder.build());
    }
  }

  @RestController
  static class RelayController {

    private final RestClient client;

    RelayController(RestClient client) {
      this.client = client;
    }

    @GetMapping("/relay")
    void relay(@RequestParam String url) {
      client.get().uri(url).retrieve().toBodilessEntity();
    }
  }
}
