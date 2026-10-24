package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.mockserver.MockServerContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

class ZbarSharedContainersTest {

  private final ApplicationContextRunner runner = ZbarContainersRunner.create();

  @Test
  void should_share_one_container_of_each_type_when_contexts_differ() {
    runner.run(
        first ->
            runner
                .withPropertyValues("another.context=true")
                .run(
                    second -> {
                      assertThat(second.getBean(PostgreSQLContainer.class))
                          .isSameAs(first.getBean(PostgreSQLContainer.class));
                      assertThat(second.getBean(MockServerContainer.class))
                          .isSameAs(first.getBean(MockServerContainer.class));
                    }));
  }

  @Test
  void should_keep_shared_containers_running_when_another_context_closes() {
    ApplicationContextRunner startingRunner =
        runner.withInitializer(new TestcontainersLifecycleApplicationContextInitializer());

    startingRunner.run(
        first ->
            startingRunner
                .withPropertyValues("another.context=true")
                .run(
                    second -> {
                      first
                          .getSourceApplicationContext(ConfigurableApplicationContext.class)
                          .close();

                      assertThat(second.getBean(PostgreSQLContainer.class).isRunning()).isTrue();
                      assertThat(second.getBean(MockServerContainer.class).isRunning()).isTrue();
                    }));
  }
}
