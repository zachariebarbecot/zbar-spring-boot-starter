package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mockserver.MockServerContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ZbarSpringBootTest
class ZbarCustomContainersIT {

  @Autowired ApplicationContext context;

  @Test
  void should_not_create_starter_containers_when_the_test_declares_its_own() {
    assertThat(context.getBeansOfType(PostgreSQLContainer.class)).isEmpty();
    assertThat(context.getBeansOfType(JdbcConnectionDetails.class)).containsOnlyKeys("customJdbc");
    assertThat(context.getBeansOfType(MockServerContainer.class))
        .containsOnlyKeys("customMockServer");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class CustomContainersConfiguration {

    @Bean
    JdbcConnectionDetails customJdbc() {
      return new JdbcConnectionDetails() {
        @Override
        public String getJdbcUrl() {
          return "jdbc:postgresql://localhost:1/custom";
        }

        @Override
        public String getUsername() {
          return "custom";
        }

        @Override
        public String getPassword() {
          return "custom";
        }
      };
    }

    @Bean
    MockServerContainer customMockServer() {
      return new MockServerContainer(
          DockerImageName.parse(ZbarMockServerConfiguration.DEFAULT_IMAGE));
    }
  }
}
