package fr.zbar.starter.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionsReadIT {

  @Test
  void should_run_in_read_only_transaction_and_return_value_when_read() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);

      Boolean readOnly =
          transactions.read(TransactionSynchronizationManager::isCurrentTransactionReadOnly);

      assertThat(readOnly).isTrue();
    }
  }

  @Test
  void should_open_read_only_connection_when_read() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      DataSource dataSource = context.getBean(DataSource.class);

      Boolean readOnly =
          transactions.read(
              () -> {
                try {
                  return DataSourceUtils.getConnection(dataSource).isReadOnly();
                } catch (SQLException e) {
                  throw new IllegalStateException(e);
                }
              });

      assertThat(readOnly).isTrue();
    }
  }

  @Test
  void should_rethrow_same_exception_when_read_throws() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      RuntimeException failure = new IllegalStateException("boom");

      assertThatThrownBy(
              () ->
                  transactions.read(
                      () -> {
                        throw failure;
                      }))
          .isSameAs(failure);
    }
  }

  @Test
  void should_return_null_when_read_returns_null() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      AtomicBoolean invoked = new AtomicBoolean();

      Object result =
          transactions.read(
              () -> {
                invoked.set(true);
                return null;
              });

      assertThat(invoked).isTrue();
      assertThat(result).isNull();
    }
  }

  @Test
  void should_join_write_transaction_and_see_rows_when_read_inside_execute() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
      AtomicBoolean readOnly = new AtomicBoolean(true);
      AtomicInteger visibleRows = new AtomicInteger();

      transactions.execute(
          () -> {
            insert(jdbc, "a");
            return transactions.read(
                () -> {
                  readOnly.set(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
                  visibleRows.set(count(jdbc));
                  return null;
                });
          });

      assertThat(readOnly).isFalse();
      assertThat(visibleRows).hasValue(1);
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
