package fr.zbar.starter.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class TransactionsActivationIT {

  @Test
  void should_not_create_transactions_bean_when_disabled() {
    try (ConfigurableApplicationContext context =
        run(PlainApplication.class, "zbar.transaction.enabled=false")) {
      assertThat(context.getBeansOfType(Transactions.class)).isEmpty();
    }
  }

  private ConfigurableApplicationContext run(Class<?> application, String... properties) {
    return new SpringApplicationBuilder(application)
        .web(WebApplicationType.NONE)
        .properties("spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID())
        .properties(properties)
        .run();
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
