package fr.zbar.starter.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionsRunIT {

  @Test
  void should_run_in_write_transaction_and_persist_row_when_run_succeeds() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
      AtomicBoolean active = new AtomicBoolean();
      AtomicBoolean readOnly = new AtomicBoolean(true);

      transactions.run(
          () -> {
            active.set(TransactionSynchronizationManager.isActualTransactionActive());
            readOnly.set(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            insert(jdbc, "a");
          });

      assertThat(active).isTrue();
      assertThat(readOnly).isFalse();
      assertThat(count(jdbc)).isEqualTo(1);
    }
  }

  @Test
  void should_rollback_and_rethrow_same_exception_when_run_throws() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
      RuntimeException failure = new IllegalStateException("boom");

      assertThatThrownBy(
              () ->
                  transactions.run(
                      () -> {
                        insert(jdbc, "a");
                        throw failure;
                      }))
          .isSameAs(failure);
      assertThat(count(jdbc)).isZero();
    }
  }

  @Test
  void should_rollback_run_row_when_outer_execute_throws() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
      AtomicBoolean ran = new AtomicBoolean();

      assertThatThrownBy(
              () ->
                  transactions.execute(
                      () -> {
                        transactions.run(
                            () -> {
                              insert(jdbc, "a");
                              ran.set(true);
                            });
                        throw new IllegalStateException("outer");
                      }))
          .isInstanceOf(IllegalStateException.class);
      assertThat(ran).isTrue();
      assertThat(count(jdbc)).isZero();
    }
  }

  private static void insert(JdbcTemplate jdbc, String name) {
    jdbc.update("insert into item(name) values (?)", name);
  }

  private static Integer count(JdbcTemplate jdbc) {
    return jdbc.queryForObject("select count(*) from item", Integer.class);
  }

  private ConfigurableApplicationContext run() {
    ConfigurableApplicationContext context =
        new SpringApplicationBuilder(PlainApplication.class)
            .web(WebApplicationType.NONE)
            .properties(
                "spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
            .run();
    context.getBean(JdbcTemplate.class).execute("create table item(name varchar(50))");
    return context;
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
