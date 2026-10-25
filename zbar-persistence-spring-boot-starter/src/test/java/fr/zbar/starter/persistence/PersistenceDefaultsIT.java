package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

class PersistenceDefaultsIT {

  @Test
  void should_set_jpa_defaults_when_not_overridden() {
    try (ConfigurableApplicationContext context = PersistenceTestApplication.run()) {
      var environment = context.getEnvironment();

      assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
      assertThat(environment.getProperty("spring.jpa.show-sql")).isEqualTo("false");
      assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
      assertThat(environment.getProperty("spring.jpa.hibernate.naming.physical-strategy"))
          .isEqualTo(CamelCaseToUnderscoresNamingStrategy.class.getName());
      assertThat(environment.getProperty("spring.jpa.hibernate.naming.implicit-strategy"))
          .isEqualTo(SpringImplicitNamingStrategy.class.getName());
      assertThat(property(environment, "hibernate.jdbc.time_zone")).isEqualTo("UTC");
      assertThat(property(environment, "hibernate.jdbc.batch_size")).isEqualTo("50");
      assertThat(property(environment, "hibernate.order_inserts")).isEqualTo("true");
      assertThat(property(environment, "hibernate.order_updates")).isEqualTo("true");
      assertThat(property(environment, "hibernate.default_batch_fetch_size")).isEqualTo("16");
      assertThat(property(environment, "hibernate.query.fail_on_pagination_over_collection_fetch"))
          .isEqualTo("true");
    }
  }

  @Test
  void should_set_liquibase_defaults_when_not_overridden() {
    try (ConfigurableApplicationContext context = PersistenceTestApplication.run()) {
      var environment = context.getEnvironment();

      assertThat(environment.getProperty("spring.liquibase.change-log"))
          .isEqualTo("classpath:db/changelog/db.changelog-master.yaml");
      assertThat(environment.getProperty("spring.liquibase.drop-first")).isEqualTo("false");
    }
  }

  @Test
  void should_let_explicit_ddl_auto_win_over_validate() {
    try (ConfigurableApplicationContext context =
        PersistenceTestApplication.run("--spring.jpa.hibernate.ddl-auto=update")) {
      assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
          .isEqualTo("update");
    }
  }

  @Test
  void should_let_application_values_win_and_keep_other_defaults() {
    try (ConfigurableApplicationContext context =
        PersistenceTestApplication.run(
            "--spring.jpa.open-in-view=true",
            "--spring.jpa.properties.hibernate.jdbc.batch_size=100")) {
      var environment = context.getEnvironment();

      assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("true");
      assertThat(property(environment, "hibernate.jdbc.batch_size")).isEqualTo("100");
      assertThat(environment.getProperty("spring.jpa.show-sql")).isEqualTo("false");
      assertThat(property(environment, "hibernate.jdbc.time_zone")).isEqualTo("UTC");
    }
  }

  @Test
  void should_let_config_file_values_win_and_keep_other_defaults() {
    try (ConfigurableApplicationContext context =
        PersistenceTestApplication.run("--spring.profiles.active=override")) {
      var environment = context.getEnvironment();

      assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("true");
      assertThat(property(environment, "hibernate.jdbc.batch_size")).isEqualTo("100");
      assertThat(environment.getProperty("spring.jpa.show-sql")).isEqualTo("false");
      assertThat(property(environment, "hibernate.jdbc.time_zone")).isEqualTo("UTC");
    }
  }

  private static String property(Environment environment, String key) {
    return environment.getProperty("spring.jpa.properties." + key);
  }
}
