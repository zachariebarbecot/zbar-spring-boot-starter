package fr.zbar.starter.errors;

import static fr.zbar.starter.errors.ErrorsTestSupport.client;
import static fr.zbar.starter.errors.ErrorsTestSupport.run;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

class ErrorsAutoConfigurationIT {

  @Test
  void should_activate_starter_when_enabled_is_absent() {
    try (ConfigurableApplicationContext context = run(Application.class)) {
      assertThat(context.getBeansOfType(ErrorsAutoConfiguration.class)).hasSize(1);
      assertThat(context.getBeansOfType(ErrorsProperties.class)).hasSize(1);
      assertThat(context.getBeansOfType(ErrorsExceptionHandler.class)).hasSize(1);
    }
  }

  @Test
  void should_create_no_starter_bean_when_enabled_is_false() {
    try (ConfigurableApplicationContext context =
        run(Application.class, "zbar.errors.enabled=false")) {
      assertThat(context.getBeansOfType(ErrorsAutoConfiguration.class)).isEmpty();
      assertThat(context.getBeansOfType(ErrorsProperties.class)).isEmpty();
      assertThat(context.getBeansOfType(ErrorsExceptionHandler.class)).isEmpty();
    }
  }

  @Test
  void
      should_answer_one_problem_json_without_bean_conflict_when_spring_problem_details_are_enabled() {
    ResponseEntity<String> response;
    try (ConfigurableApplicationContext context =
        run(Application.class, "spring.mvc.problemdetails.enabled=true")) {
      assertThat(context.getBeansOfType(ResponseEntityExceptionHandler.class)).hasSize(1);
      assertThat(context.getBeansOfType(ErrorsExceptionHandler.class)).hasSize(1);
      response = client(context).get().uri("/unknown").retrieve().toEntity(String.class);
    }

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(SampleController.class)
  static class Application {}
}
