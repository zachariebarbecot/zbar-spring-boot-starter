package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

class ZbarPostgresImageIT {

  @Nested
  @ZbarPostgresTest
  @TestPropertySource(properties = "zbar.test.postgres.image=postgres:18-alpine")
  class WithConfiguredImage {

    @Autowired PostgreSQLContainer container;

    @Test
    void should_use_configured_image_when_property_is_set() {
      assertThat(container.getDockerImageName()).isEqualTo("postgres:18-alpine");
    }
  }

  @Nested
  @ZbarPostgresTest
  class WithDefaultImage {

    @Autowired PostgreSQLContainer container;

    @Test
    void should_use_pinned_default_image_when_property_is_absent() {
      assertThat(container.getDockerImageName()).isEqualTo("postgres:17.6");
    }
  }
}
