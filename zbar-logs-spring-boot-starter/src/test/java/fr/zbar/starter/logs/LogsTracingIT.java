package fr.zbar.starter.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({OutputCaptureExtension.class, ClearStructuredFormatExtension.class})
class LogsTracingIT {

  private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
  private static final String VALID_TRACEPARENT = "00-" + TRACE_ID + "-b7ad6b7169203331-01";
  private static final Pattern CONSOLE_CORRELATION =
      Pattern.compile("\\[([0-9a-f]{32}-[0-9a-f]{16})] .*hello-from-controller");

  @Test
  void should_reuse_incoming_trace_id_and_log_a_span_id(CapturedOutput output) {
    try (ConfigurableApplicationContext context = run()) {
      call(context, VALID_TRACEPARENT);
    }

    Matcher correlation = CONSOLE_CORRELATION.matcher(output);
    assertThat(correlation.find()).isTrue();
    assertThat(correlation.group(1)).startsWith(TRACE_ID + "-").matches(".*-[0-9a-f]{16}");
  }

  @Test
  void should_create_trace_id_when_traceparent_is_absent(CapturedOutput output) {
    try (ConfigurableApplicationContext context = run()) {
      call(context, null);
    }

    Matcher correlation = CONSOLE_CORRELATION.matcher(output);
    assertThat(correlation.find()).isTrue();
    assertThat(correlation.group(1)).matches("[0-9a-f]{32}-[0-9a-f]{16}");
  }

  @Test
  void should_answer_normally_and_create_trace_id_when_traceparent_is_invalid(
      CapturedOutput output) {
    ResponseEntity<String> response;
    try (ConfigurableApplicationContext context = run()) {
      response = call(context, "not-a-traceparent");
    }

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isEqualTo("ok");
    Matcher correlation = CONSOLE_CORRELATION.matcher(output);
    assertThat(correlation.find()).isTrue();
    assertThat(correlation.group(1)).matches("[0-9a-f]{32}-[0-9a-f]{16}");
  }

  @Test
  void should_expose_trace_id_and_span_id_in_json_log(CapturedOutput output) {
    try (ConfigurableApplicationContext context =
        run("logging.structured.format.console=logstash")) {
      call(context, VALID_TRACEPARENT);
    }

    JsonNode event =
        Arrays.stream(output.getOut().split("\n"))
            .filter(line -> line.startsWith("{") && line.contains("hello-from-controller"))
            .map(line -> JsonMapper.shared().readTree(line))
            .findFirst()
            .orElseThrow();
    assertThat(event.get("traceId").asString()).isEqualTo(TRACE_ID);
    assertThat(event.get("spanId").asString()).matches("[0-9a-f]{16}");
  }

  @Test
  void should_add_no_correlation_header_to_response() {
    ResponseEntity<String> response;
    try (ConfigurableApplicationContext context = run()) {
      response = call(context, VALID_TRACEPARENT);
    }

    assertThat(response.getHeaders().headerNames())
        .noneMatch(name -> name.equalsIgnoreCase("traceparent"))
        .noneMatch(name -> name.toLowerCase().contains("trace"))
        .noneMatch(name -> name.toLowerCase().contains("correlation"));
  }

  @Test
  void should_propagate_trace_id_to_async_method(CapturedOutput output) {
    try (ConfigurableApplicationContext context = runAsync()) {
      call(context, VALID_TRACEPARENT, "/async");
    }

    assertThat(jsonEvent(output, "hello-from-async").get("traceId").asString()).isEqualTo(TRACE_ID);
  }

  @Test
  void should_apply_application_task_decorator_and_propagate_trace_id(CapturedOutput output) {
    int calls;
    try (ConfigurableApplicationContext context =
        runAsync(new Class<?>[] {AsyncApplication.class, CountingDecoratorConfig.class})) {
      call(context, VALID_TRACEPARENT, "/async");
      calls = context.getBean(CountingDecorator.class).calls.get();
    }

    assertThat(calls).isPositive();
    assertThat(jsonEvent(output, "hello-from-async").get("traceId").asString()).isEqualTo(TRACE_ID);
  }

  @Test
  void should_not_leak_trace_id_to_next_task_on_same_thread(CapturedOutput output) {
    try (ConfigurableApplicationContext context =
        runAsync(
            "spring.task.execution.pool.core-size=1", "spring.task.execution.pool.max-size=1")) {
      call(context, VALID_TRACEPARENT, "/async");
      context.getBean(AsyncService.class).hello().join();
    }

    List<JsonNode> events = jsonEvents(output, "hello-from-async");
    assertThat(events.getFirst().get("traceId").asString()).isEqualTo(TRACE_ID);
    JsonNode second = events.get(1);
    assertThat(second.has("traceId")).isFalse();
  }

  private JsonNode jsonEvent(CapturedOutput output, String message) {
    return jsonEvents(output, message).getFirst();
  }

  private List<JsonNode> jsonEvents(CapturedOutput output, String message) {
    return Arrays.stream(output.getOut().split("\n"))
        .filter(line -> line.startsWith("{") && line.contains(message))
        .map(line -> JsonMapper.shared().readTree(line))
        .toList();
  }

  private ConfigurableApplicationContext runAsync(String... properties) {
    return runAsync(new Class<?>[] {AsyncApplication.class}, properties);
  }

  private ConfigurableApplicationContext runAsync(Class<?>[] sources, String... properties) {
    return new SpringApplicationBuilder(sources)
        .web(WebApplicationType.SERVLET)
        .properties("server.port=0", "logging.structured.format.console=logstash")
        .properties(properties)
        .run();
  }

  @Configuration
  static class CountingDecoratorConfig {

    @Bean
    CountingDecorator countingDecorator() {
      return new CountingDecorator();
    }
  }

  static class CountingDecorator implements TaskDecorator {

    final AtomicInteger calls = new AtomicInteger();

    @Override
    public Runnable decorate(Runnable runnable) {
      calls.incrementAndGet();
      return runnable;
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @EnableAsync
  static class AsyncApplication {

    @Bean
    AsyncService asyncService() {
      return new AsyncService();
    }

    @Bean
    AsyncController asyncController(AsyncService service) {
      return new AsyncController(service);
    }
  }

  static class AsyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncService.class);

    @Async
    CompletableFuture<Void> hello() {
      LOGGER.info("hello-from-async");
      return CompletableFuture.completedFuture(null);
    }
  }

  @RestController
  static class AsyncController {

    private final AsyncService service;

    AsyncController(AsyncService service) {
      this.service = service;
    }

    @GetMapping("/async")
    String async() {
      service.hello().join();
      return "ok";
    }
  }

  private ConfigurableApplicationContext run(String... properties) {
    return new SpringApplicationBuilder(TracedApplication.class)
        .web(WebApplicationType.SERVLET)
        .properties("server.port=0")
        .properties(properties)
        .run();
  }

  private ResponseEntity<String> call(ConfigurableApplicationContext context, String traceparent) {
    return call(context, traceparent, "/hello");
  }

  private ResponseEntity<String> call(
      ConfigurableApplicationContext context, String traceparent, String path) {
    String port = context.getEnvironment().getProperty("local.server.port");
    RestClient.RequestHeadersSpec<?> request =
        RestClient.create("http://localhost:" + port).get().uri(path);
    if (traceparent != null) {
      request = request.header("traceparent", traceparent);
    }
    return request.retrieve().toEntity(String.class);
  }

  @Configuration
  @EnableAutoConfiguration
  static class TracedApplication {

    @Bean
    HelloController helloController() {
      return new HelloController();
    }
  }

  @RestController
  static class HelloController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    String hello() {
      LOGGER.info("hello-from-controller");
      return "ok";
    }
  }
}
