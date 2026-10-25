package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ConfigurableApplicationContext;

class PersistenceActivationIT {

  @ParameterizedTest
  @ValueSource(
      strings = {"--zbar.persistence.enabled=false", "--zbar.persistence.jpa.enabled=false"})
  void should_add_no_jpa_default_when_disabled(String switchOff) {
    try (ConfigurableApplicationContext context = PersistenceTestApplication.run(switchOff)) {
      var environment = context.getEnvironment();

      assertThat(environment.getProperty("spring.jpa.open-in-view")).isNull();
      assertThat(environment.getProperty("spring.jpa.hibernate.naming.physical-strategy")).isNull();
      assertThat(environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size"))
          .isNull();
    }
  }

  @Test
  void should_add_no_default_at_all_when_persistence_is_disabled() {
    try (ConfigurableApplicationContext context =
        PersistenceTestApplication.run("--zbar.persistence.enabled=false")) {
      assertThat(context.getEnvironment().getProperty("spring.liquibase.change-log")).isNull();
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"--spring.liquibase.enabled=false", "--zbar.persistence.liquibase.enabled=false"})
  void should_use_ddl_auto_none_and_no_liquibase_default_when_liquibase_is_off(String switchOff) {
    try (ConfigurableApplicationContext context = PersistenceTestApplication.run(switchOff)) {
      var environment = context.getEnvironment();

      assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("none");
      assertThat(environment.getProperty("spring.liquibase.change-log")).isNull();
      assertThat(environment.getProperty("spring.liquibase.drop-first")).isNull();
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"--spring.liquibase.enabled=false", "--zbar.persistence.liquibase.enabled=false"})
  void should_let_explicit_ddl_auto_win_when_liquibase_is_off(String switchOff) {
    try (ConfigurableApplicationContext context =
        PersistenceTestApplication.run(switchOff, "--spring.jpa.hibernate.ddl-auto=update")) {
      assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
          .isEqualTo("update");
    }
  }
}
