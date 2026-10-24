package fr.zbar.starter.errors;

import static fr.zbar.starter.errors.ErrorsTestSupport.client;
import static fr.zbar.starter.errors.ErrorsTestSupport.run;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

class ErrorsAdviceOverrideIT {

  @Test
  void should_answer_from_application_advice_when_it_handles_the_exception() {
    ResponseEntity<String> response;
    try (ConfigurableApplicationContext context = run(Application.class)) {
      response = client(context).get().uri("/teapot").retrieve().toEntity(String.class);
      assertThat(context.getBeansOfType(ErrorsExceptionHandler.class)).isEmpty();
    }

    assertThat(response.getStatusCode().value()).isEqualTo(418);
    assertThat(response.getBody()).isEqualTo("handled by application");
  }

  @RestControllerAdvice
  static class ApplicationAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(SampleController.TeapotException.class)
    ResponseEntity<String> handle() {
      return ResponseEntity.status(418).body("handled by application");
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({SampleController.class, ApplicationAdvice.class})
  static class Application {}
}
