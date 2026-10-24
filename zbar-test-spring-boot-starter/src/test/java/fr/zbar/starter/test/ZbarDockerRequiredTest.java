package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ZbarDockerRequiredTest {

  @Test
  void should_fail_to_start_with_explicit_message_when_docker_is_unavailable() {
    new ApplicationContextRunner()
        .withUserConfiguration(
            DockerUnavailable.class,
            ZbarPostgresConfiguration.class,
            ZbarMockServerConfiguration.class)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("Docker is required"));
  }

  @Configuration(proxyBeanMethods = false)
  static class DockerUnavailable {

    @Bean
    ZbarDockerAvailability dockerAvailability() {
      return () -> false;
    }
  }
}
