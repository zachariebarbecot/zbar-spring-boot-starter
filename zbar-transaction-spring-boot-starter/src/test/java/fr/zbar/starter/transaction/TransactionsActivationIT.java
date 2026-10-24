package fr.zbar.starter.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class TransactionsActivationIT {

  @Test
  void should_not_create_transactions_bean_when_disabled() {
    try (ConfigurableApplicationContext context =
        run(PlainApplication.class, "zbar.transaction.enabled=false")) {
      assertThat(context.getBeansOfType(Transactions.class)).isEmpty();
    }
  }

  @Test
  void should_inject_application_transactions_bean_and_create_no_other() {
    try (ConfigurableApplicationContext context = run(CustomTransactionsApplication.class)) {
      assertThat(context.getBeansOfType(Transactions.class)).hasSize(1);
      assertThat(context.getBean(Transactions.class))
          .isSameAs(context.getBean(CustomTransactionsApplication.class).custom);
    }
  }

  @Test
  void should_start_without_transactions_bean_when_several_managers_and_no_primary() {
    try (ConfigurableApplicationContext context = run(TwoManagersApplication.class)) {
      assertThat(context.getBeansOfType(Transactions.class)).isEmpty();
    }
  }

  @Test
  void should_use_primary_manager_when_several_managers() {
    try (ConfigurableApplicationContext context = run(PrimaryManagerApplication.class)) {
      Transactions transactions = context.getBean(Transactions.class);
      PrimaryManagerApplication application = context.getBean(PrimaryManagerApplication.class);

      transactions.execute(() -> null);

      assertThat(application.primary.begun).isTrue();
      assertThat(application.secondary.begun).isFalse();
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

  @Configuration
  @EnableAutoConfiguration
  static class CustomTransactionsApplication {
    final Transactions custom = new Transactions(new RecordingManager());

    @Bean
    Transactions customTransactions() {
      return custom;
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class TwoManagersApplication {
    @Bean
    PlatformTransactionManager first() {
      return new RecordingManager();
    }

    @Bean
    PlatformTransactionManager second() {
      return new RecordingManager();
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class PrimaryManagerApplication {
    final RecordingManager primary = new RecordingManager();
    final RecordingManager secondary = new RecordingManager();

    @Bean
    @Primary
    PlatformTransactionManager primaryManager() {
      return primary;
    }

    @Bean
    PlatformTransactionManager secondaryManager() {
      return secondary;
    }
  }

  static class RecordingManager extends AbstractPlatformTransactionManager {
    boolean begun;

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
      begun = true;
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
