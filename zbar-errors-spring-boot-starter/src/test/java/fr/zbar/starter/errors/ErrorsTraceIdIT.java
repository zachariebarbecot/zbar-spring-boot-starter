package fr.zbar.starter.errors;

import static fr.zbar.starter.errors.ErrorsTestSupport.client;
import static fr.zbar.starter.errors.ErrorsTestSupport.run;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.tracing.Tracer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class ErrorsTraceIdIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
  private static final String TRACEPARENT = "00-" + TRACE_ID + "-b7ad6b7169203331-01";
  private static final String[] SAMPLED = {"management.tracing.sampling.probability=1.0"};

  private final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @AfterEach
  void detachAppender() {
    rootLogger.detachAppender(appender);
  }

  @Test
  void should_expose_current_span_trace_id_when_client_error_is_handled() {
    TracedController.spanTraceId.set(null);

    ResponseEntity<String> response = get("/traced-conflict", null, SAMPLED);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(TracedController.spanTraceId.get()).matches("[0-9a-f]{32}");
    assertThat(body(response)).containsEntry("traceId", TracedController.spanTraceId.get());
  }

  @Test
  void should_expose_current_span_trace_id_when_server_error_is_handled() {
    TracedController.spanTraceId.set(null);

    ResponseEntity<String> response = get("/boom", null, SAMPLED);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(body(response)).extractingByKey("traceId").asString().matches("[0-9a-f]{32}");
  }

  @Test
  void should_expose_current_span_trace_id_when_not_found_is_handled() {
    ResponseEntity<String> response = get("/unknown", null, SAMPLED);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(body(response)).extractingByKey("traceId").asString().matches("[0-9a-f]{32}");
  }

  @Test
  void should_expose_incoming_traceparent_trace_id_shared_with_error_log() {
    ResponseEntity<String> response = get("/boom", TRACEPARENT, SAMPLED);

    assertThat(body(response)).containsEntry("traceId", TRACE_ID);
    ILoggingEvent error =
        appender.list.stream()
            .filter(event -> event.getLevel() == Level.ERROR)
            .findFirst()
            .orElseThrow();
    assertThat(error.getMDCPropertyMap()).containsEntry("traceId", TRACE_ID);
  }

  @Test
  void should_omit_trace_id_when_no_valid_span_exists() {
    ResponseEntity<String> response =
        get("/boom", null, "management.observations.enable.http.server.requests=false");

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).doesNotContain("traceId");
    assertThat(body(response)).containsEntry("status", 500).doesNotContainKey("traceId");
  }

  @Test
  void should_answer_without_trace_id_when_no_tracer_bean_exists() {
    ResponseEntity<String> response =
        get(
            "/boom",
            null,
            "spring.autoconfigure.exclude="
                + "org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure."
                + "OpenTelemetryTracingAutoConfiguration,"
                + "org.springframework.boot.micrometer.tracing.autoconfigure."
                + "NoopTracerAutoConfiguration,"
                + "org.springframework.boot.micrometer.tracing.autoconfigure."
                + "MicrometerTracingAutoConfiguration");

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).doesNotContain("traceId");
    assertThat(body(response)).containsEntry("status", 500);
  }

  @Test
  void should_omit_trace_id_when_tracer_is_noop() {
    ResponseEntity<String> response =
        get(
            "/boom",
            null,
            "spring.autoconfigure.exclude="
                + "org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure."
                + "OpenTelemetryTracingAutoConfiguration");

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).doesNotContain("traceId");
  }

  private static Map<String, Object> body(ResponseEntity<String> response) {
    return JSON.readValue(response.getBody(), new TypeReference<>() {});
  }

  private ResponseEntity<String> get(String path, String traceparent, String... properties) {
    try (ConfigurableApplicationContext context = run(Application.class, properties)) {
      appender.start();
      rootLogger.addAppender(appender);
      RestClient.RequestHeadersSpec<?> request = client(context).get().uri(path);
      if (traceparent != null) {
        request = request.header("traceparent", traceparent);
      }
      return request.retrieve().toEntity(String.class);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(SampleController.class)
  static class Application {

    @Bean
    TracedController tracedController(ObjectProvider<Tracer> tracer) {
      return new TracedController(tracer);
    }
  }

  @RestController
  static class TracedController {

    static final AtomicReference<String> spanTraceId = new AtomicReference<>();

    private final ObjectProvider<Tracer> tracer;

    TracedController(ObjectProvider<Tracer> tracer) {
      this.tracer = tracer;
    }

    @GetMapping("/traced-conflict")
    String conflict() {
      spanTraceId.set(tracer.getObject().currentSpan().context().traceId());
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
  }
}
