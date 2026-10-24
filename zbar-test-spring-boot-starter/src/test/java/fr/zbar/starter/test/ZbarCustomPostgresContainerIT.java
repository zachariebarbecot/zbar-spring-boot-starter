package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ZbarSpringBootTest
class ZbarCustomPostgresContainerIT {

  @Autowired ApplicationContext context;
  @Autowired DataSource dataSource;
  @Autowired PostgreSQLContainer container;

  @Test
  void should_use_the_test_postgres_container_when_the_test_declares_its_own() throws SQLException {
    assertThat(context.getBeansOfType(PostgreSQLContainer.class))
        .containsOnlyKeys("customPostgres");
    try (var connection = dataSource.getConnection()) {
      assertThat(connection.getMetaData().getURL()).isEqualTo(container.getJdbcUrl());
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class CustomPostgresConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer customPostgres() {
      return new PostgreSQLContainer("postgres:17.6");
    }
  }
}
