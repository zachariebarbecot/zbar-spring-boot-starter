package fr.zbar.starter.test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
@Import(ZbarDockerConfiguration.class)
class ZbarPostgresConfiguration {

  static final String DEFAULT_IMAGE = "postgres:17.6";

  @Bean
  @ServiceConnection
  @ConditionalOnMissingBean(
      value = PostgreSQLContainer.class,
      type = "org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails")
  PostgreSQLContainer postgresContainer(
      Environment environment, ZbarDockerAvailability dockerAvailability) {
    ZbarDocker.require(dockerAvailability::isAvailable);
    return ZbarContainers.postgres(
        environment.getProperty("zbar.test.postgres.image", DEFAULT_IMAGE),
        ZbarContainers.reuse(environment));
  }
}
