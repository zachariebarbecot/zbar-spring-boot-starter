package fr.zbar.starter.errors;

import static fr.zbar.starter.errors.ErrorsTestSupport.client;
import static fr.zbar.starter.errors.ErrorsTestSupport.run;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

class ErrorsStatusIT {

  private final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @AfterEach
  void detachAppender() {
    rootLogger.detachAppender(appender);
  }

  @Test
  void should_keep_status_and_not_log_error_when_exception_is_annotated_with_response_status() {
    ResponseEntity<String> response = get("/missing");

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(appender.list).noneMatch(event -> event.getLevel() == Level.ERROR);
  }

  private ResponseEntity<String> get(String path) {
    try (ConfigurableApplicationContext context = run(Application.class)) {
      appender.start();
      rootLogger.addAppender(appender);
      return client(context).get().uri(path).retrieve().toEntity(String.class);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(StatusController.class)
  static class Application {}

  @RestController
  static class StatusController {

    @GetMapping("/missing")
    String missing() {
      throw new MissingException();
    }
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  static class MissingException extends RuntimeException {}
}
