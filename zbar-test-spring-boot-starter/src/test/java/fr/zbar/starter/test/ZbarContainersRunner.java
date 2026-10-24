package fr.zbar.starter.test;

import static org.mockito.Mockito.mock;

import org.mockserver.client.MockServerClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Context runner on both starter configurations, without a client bound to an unstarted container.
 */
final class ZbarContainersRunner {

  private ZbarContainersRunner() {}

  static ApplicationContextRunner create() {
    return new ApplicationContextRunner()
        .withUserConfiguration(
            ClientWithoutContainer.class,
            ZbarPostgresConfiguration.class,
            ZbarMockServerConfiguration.class);
  }

  /** Declared first so the starter does not connect a client to an unstarted container. */
  @Configuration(proxyBeanMethods = false)
  static class ClientWithoutContainer {

    @Bean
    MockServerClient mockServerClient() {
      return mock(MockServerClient.class);
    }
  }
}
