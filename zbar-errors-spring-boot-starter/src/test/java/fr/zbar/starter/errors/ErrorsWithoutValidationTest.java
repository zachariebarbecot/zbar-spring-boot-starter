package fr.zbar.starter.errors;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ErrorsWithoutValidationTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ErrorsAutoConfiguration.class));

  @Test
  void should_start_when_jakarta_validation_is_absent() {
    runner
        .withClassLoader(new FilteredClassLoader(ConstraintViolationException.class))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(ErrorsExceptionHandler.class);
            });
  }
}
