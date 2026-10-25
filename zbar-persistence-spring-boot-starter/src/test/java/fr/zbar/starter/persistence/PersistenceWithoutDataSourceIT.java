package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class PersistenceWithoutDataSourceIT {

  @Test
  void should_start_without_entity_manager_factory_nor_liquibase() {
    try (ConfigurableApplicationContext context =
        PersistenceTestApplication.runWithoutDataSource()) {
      assertThat(context.getBeansOfType(EntityManagerFactory.class)).isEmpty();
      assertThat(context.getBeansOfType(SpringLiquibase.class)).isEmpty();
    }
  }
}
