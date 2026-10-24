package fr.zbar.starter.errors;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ErrorsWithoutTracingTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ErrorsAutoConfiguration.class));

  @Test
  void should_start_and_resolve_no_trace_id_when_micrometer_tracing_is_absent() {
    runner
        .withClassLoader(new FilteredClassLoader(Tracer.class))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(TraceIdResolver.class).currentTraceId()).isNull();
              assertThat(context).hasSingleBean(ErrorsExceptionHandler.class);
            });
  }
}
