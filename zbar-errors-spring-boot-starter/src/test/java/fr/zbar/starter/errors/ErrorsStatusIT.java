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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.DelegatingFilterProxy;

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

  @Test
  void should_answer_403_when_access_is_denied() {
    ResponseEntity<String> response = get("/denied", SecuredApplication.class);

    assertThat(response.getStatusCode().value()).isEqualTo(403);
    assertThat(appender.list).noneMatch(event -> event.getLevel() == Level.ERROR);
  }

  private ResponseEntity<String> get(String path) {
    return get(path, Application.class);
  }

  private ResponseEntity<String> get(String path, Class<?> application) {
    try (ConfigurableApplicationContext context = run(application)) {
      appender.start();
      rootLogger.addAppender(appender);
      return client(context).get().uri(path).retrieve().toEntity(String.class);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(StatusController.class)
  static class Application {}

  @Configuration
  @EnableAutoConfiguration
  @EnableWebSecurity
  @Import(StatusController.class)
  static class SecuredApplication {

    @Bean
    SecurityFilterChain permitAll(HttpSecurity http) {
      return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
    }

    @Bean
    FilterRegistrationBean<DelegatingFilterProxy> securityFilter() {
      return new FilterRegistrationBean<>(new DelegatingFilterProxy("springSecurityFilterChain"));
    }
  }

  @RestController
  static class StatusController {

    @GetMapping("/denied")
    String denied() {
      throw new AccessDeniedException("no");
    }

    @GetMapping("/missing")
    String missing() {
      throw new MissingException();
    }
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  static class MissingException extends RuntimeException {}
}
