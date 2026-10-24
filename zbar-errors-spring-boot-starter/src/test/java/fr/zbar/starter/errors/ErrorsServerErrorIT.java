package fr.zbar.starter.errors;

import static fr.zbar.starter.errors.ErrorsTestSupport.client;
import static fr.zbar.starter.errors.ErrorsTestSupport.run;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class ErrorsServerErrorIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String GENERIC_DETAIL = "An unexpected error occurred.";

  private final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @AfterEach
  void detachAppender() {
    rootLogger.detachAppender(appender);
  }

  @Test
  void should_answer_generic_problem_json_when_exception_is_unhandled() {
    ResponseEntity<String> response = get("/boom");

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(body(response))
        .containsEntry("title", "Internal Server Error")
        .containsEntry("status", 500)
        .containsEntry("detail", GENERIC_DETAIL);
    assertThat(response.getBody())
        .doesNotContain("select", "hunter2", "IllegalStateException", "fr.zbar", "at ");
  }

  @Test
  void should_log_exactly_one_error_with_stacktrace_when_exception_is_unhandled() {
    get("/boom");

    assertThat(errors()).hasSize(1);
    ILoggingEvent error = errors().getFirst();
    assertThat(error.getThrowableProxy()).isNotNull();
    assertThat(error.getThrowableProxy().getClassName())
        .isEqualTo(IllegalStateException.class.getName());
  }

  @Test
  void should_not_log_error_when_client_error_is_handled() {
    get("/conflict");
    get("/unknown");
    get("/param");
    send("/read-only", "POST", "{}", MediaType.APPLICATION_JSON);
    send("/json", "POST", "x", MediaType.TEXT_PLAIN);
    send("/json", "POST", "{oops", MediaType.APPLICATION_JSON);

    assertThat(errors()).isEmpty();
  }

  @Test
  void should_keep_same_body_when_boot_error_properties_expose_details() {
    ResponseEntity<String> response =
        get(
            "/boom",
            "server.error.include-stacktrace=always",
            "server.error.include-message=always",
            "server.error.include-exception=true");

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(body(response))
        .containsEntry("detail", GENERIC_DETAIL)
        .doesNotContainKeys("trace", "message", "exception");
    assertThat(response.getBody()).doesNotContain("hunter2", "IllegalStateException");
  }

  @Test
  void should_mask_reason_and_log_one_error_when_response_status_exception_is_5xx() {
    ResponseEntity<String> response = get("/unavailable");

    assertThat(response.getStatusCode().value()).isEqualTo(503);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(body(response))
        .containsEntry("title", "Service Unavailable")
        .containsEntry("status", 503)
        .containsEntry("detail", GENERIC_DETAIL);
    assertThat(response.getBody()).doesNotContain("db-internal");
    assertThat(errors()).hasSize(1);
  }

  private void captureLogs() {
    appender.start();
    rootLogger.addAppender(appender);
  }

  private List<ILoggingEvent> errors() {
    return appender.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
  }

  private static Map<String, Object> body(ResponseEntity<String> response) {
    return JSON.readValue(response.getBody(), new TypeReference<>() {});
  }

  private ResponseEntity<String> get(String path, String... properties) {
    try (ConfigurableApplicationContext context = run(Application.class, properties)) {
      captureLogs();
      return client(context).get().uri(path).retrieve().toEntity(String.class);
    }
  }

  private ResponseEntity<String> send(
      String path, String method, String body, MediaType contentType) {
    try (ConfigurableApplicationContext context = run(Application.class)) {
      captureLogs();
      return client(context)
          .method(HttpMethod.valueOf(method))
          .uri(path)
          .contentType(contentType)
          .body(body)
          .retrieve()
          .toEntity(String.class);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(SampleController.class)
  static class Application {}
}
