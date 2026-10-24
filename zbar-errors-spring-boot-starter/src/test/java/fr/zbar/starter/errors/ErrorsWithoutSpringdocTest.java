package fr.zbar.starter.errors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ErrorsWithoutSpringdocTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ErrorsAutoConfiguration.class));

  @Test
  void should_start_without_openapi_customizer_when_springdoc_is_absent() {
    runner
        .withClassLoader(new FilteredClassLoader(OpenApiCustomizer.class))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(ErrorsExceptionHandler.class);
              assertThat(context).doesNotHaveBean(ErrorsOpenApiCustomizer.class);
            });
  }
}
