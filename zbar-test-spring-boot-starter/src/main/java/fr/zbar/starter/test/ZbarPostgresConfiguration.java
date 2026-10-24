package fr.zbar.starter.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class ZbarPostgresConfiguration {

  static final String DEFAULT_IMAGE = "postgres:17.6";

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer(Environment environment) {
    return new PostgreSQLContainer(
        environment.getProperty("zbar.test.postgres.image", DEFAULT_IMAGE));
  }
}
