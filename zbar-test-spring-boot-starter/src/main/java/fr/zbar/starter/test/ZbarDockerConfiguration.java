package fr.zbar.starter.test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.DockerClientFactory;

@TestConfiguration(proxyBeanMethods = false)
class ZbarDockerConfiguration {

  @Bean
  @ConditionalOnMissingBean
  ZbarDockerAvailability dockerAvailability() {
    return DockerClientFactory.instance()::isDockerAvailable;
  }
}
