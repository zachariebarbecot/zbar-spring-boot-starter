package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.mockserver.MockServerContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

class ZbarReuseTest {

  private final ApplicationContextRunner runner = ZbarContainersRunner.create();

  @Test
  void should_apply_reuse_to_both_containers_when_property_is_true() {
    runner
        .withPropertyValues("zbar.test.reuse=true")
        .run(
            context -> {
              assertThat(context.getBean(PostgreSQLContainer.class).isShouldBeReused()).isTrue();
              assertThat(context.getBean(MockServerContainer.class).isShouldBeReused()).isTrue();
            });
  }

  @Test
  void should_not_apply_reuse_when_property_is_absent() {
    runner.run(
        context -> {
          assertThat(context.getBean(PostgreSQLContainer.class).isShouldBeReused()).isFalse();
          assertThat(context.getBean(MockServerContainer.class).isShouldBeReused()).isFalse();
        });
  }
}
