package fr.zbar.starter.test;

import org.mockserver.client.MockServerClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.mockserver.MockServerContainer;

@TestConfiguration(proxyBeanMethods = false)
@Import(ZbarDockerConfiguration.class)
class ZbarMockServerConfiguration {

  static final String DEFAULT_IMAGE = "mockserver/mockserver:5.15.0";

  @Bean
  @ConditionalOnMissingBean
  MockServerContainer mockServerContainer(
      Environment environment, ZbarDockerAvailability dockerAvailability) {
    ZbarDocker.require(dockerAvailability::isAvailable);
    return ZbarContainers.mockServer(
        environment.getProperty("zbar.test.mockserver.image", DEFAULT_IMAGE),
        ZbarContainers.reuse(environment));
  }

  @Bean
  DynamicPropertyRegistrar mockServerProperties(MockServerContainer container) {
    return registry -> {
      registry.add("zbar.test.mockserver.url", container::getEndpoint);
      registry.add("zbar.test.mockserver.host", container::getHost);
      registry.add("zbar.test.mockserver.port", container::getServerPort);
    };
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean
  MockServerClient mockServerClient(MockServerContainer container) {
    return new MockServerClient(container.getHost(), container.getServerPort());
  }
}
