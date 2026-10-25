package fr.zbar.starter.persistence;

import java.util.HashMap;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@Order(Ordered.LOWEST_PRECEDENCE)
class PersistenceEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (isDisabled(environment, "zbar.persistence.enabled")) {
      return;
    }
    boolean liquibaseActive =
        !isDisabled(environment, "spring.liquibase.enabled")
            && !isDisabled(environment, "zbar.persistence.liquibase.enabled");
    Map<String, Object> defaults = new HashMap<>();
    if (!isDisabled(environment, "zbar.persistence.jpa.enabled")) {
      addJpaDefaults(defaults, liquibaseActive);
    }
    if (liquibaseActive) {
      defaults.put(
          "spring.liquibase.change-log", "classpath:db/changelog/db.changelog-master.yaml");
      defaults.put("spring.liquibase.drop-first", "false");
    }
    environment
        .getPropertySources()
        .addLast(new MapPropertySource("zbarPersistenceDefaults", defaults));
  }

  private static void addJpaDefaults(Map<String, Object> defaults, boolean liquibaseActive) {
    defaults.put("spring.jpa.open-in-view", "false");
    defaults.put("spring.jpa.show-sql", "false");
    defaults.put("spring.jpa.hibernate.ddl-auto", liquibaseActive ? "validate" : "none");
    defaults.put(
        "spring.jpa.hibernate.naming.physical-strategy",
        CamelCaseToUnderscoresNamingStrategy.class.getName());
    defaults.put(
        "spring.jpa.hibernate.naming.implicit-strategy",
        SpringImplicitNamingStrategy.class.getName());
    defaults.put("spring.jpa.properties.hibernate.jdbc.time_zone", "UTC");
    defaults.put("spring.jpa.properties.hibernate.jdbc.batch_size", "50");
    defaults.put("spring.jpa.properties.hibernate.order_inserts", "true");
    defaults.put("spring.jpa.properties.hibernate.order_updates", "true");
    defaults.put("spring.jpa.properties.hibernate.default_batch_fetch_size", "16");
    defaults.put(
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch", "true");
  }

  private static boolean isDisabled(ConfigurableEnvironment environment, String key) {
    return "false".equalsIgnoreCase(environment.getProperty(key));
  }
}
