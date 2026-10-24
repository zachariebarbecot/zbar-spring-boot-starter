package fr.zbar.starter.transaction;

import static fr.zbar.starter.transaction.TransactionsTestApplication.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;

class TransactionsExecuteIT {

  @Test
  void should_persist_row_and_return_result_when_execute_succeeds() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);

      Integer result =
          transactions.execute(
              () -> {
                insert(jdbc, "a");
                return 42;
              });

      assertThat(result).isEqualTo(42);
      assertThat(count(jdbc)).isEqualTo(1);
    }
  }

  @Test
  void should_rollback_and_rethrow_same_exception_when_execute_throws() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
      RuntimeException failure = new IllegalStateException("boom");

      assertThatThrownBy(
              () ->
                  transactions.execute(
                      () -> {
                        insert(jdbc, "a");
                        throw failure;
                      }))
          .isSameAs(failure);
      assertThat(count(jdbc)).isZero();
    }
  }

  @Test
  void should_return_null_and_persist_row_when_execute_returns_null() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);

      Object result =
          transactions.execute(
              () -> {
                insert(jdbc, "a");
                return null;
              });

      assertThat(result).isNull();
      assertThat(count(jdbc)).isEqualTo(1);
    }
  }

  @Test
  void should_start_without_transactions_bean_when_no_transaction_manager() {
    try (ConfigurableApplicationContext context = runWithoutDataSource()) {
      assertThat(context.getBeansOfType(Transactions.class)).isEmpty();
    }
  }

  @Test
  void should_throw_unexpected_rollback_when_inner_execute_fails_and_outer_catches() {
    try (ConfigurableApplicationContext context = run()) {
      Transactions transactions = context.getBean(Transactions.class);
      JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);

      assertThatThrownBy(
              () ->
                  transactions.execute(
                      () -> {
                        insert(jdbc, "a");
                        assertThatThrownBy(
                            () ->
                                transactions.execute(
                                    () -> {
                                      throw new IllegalStateException("inner");
                                    }));
                        return null;
                      }))
          .isInstanceOf(UnexpectedRollbackException.class);
      assertThat(count(jdbc)).isZero();
    }
  }

  private static void insert(JdbcTemplate jdbc, String name) {
    jdbc.update("insert into item(name) values (?)", name);
  }

  private static Integer count(JdbcTemplate jdbc) {
    return jdbc.queryForObject("select count(*) from item", Integer.class);
  }

  private ConfigurableApplicationContext runWithoutDataSource() {
    return new SpringApplicationBuilder(TransactionsTestApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration")
        .run();
  }
}
