package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@ZbarPostgresTest
class ZbarPostgresIT {

  @Autowired JdbcTemplate jdbc;

  @Test
  void should_run_select_1_on_postgres_container_when_no_datasource_property_is_set() {
    Integer one = jdbc.queryForObject("select 1", Integer.class);

    assertThat(one).isEqualTo(1);
    assertThat(jdbc.queryForObject("select version()", String.class)).contains("PostgreSQL");
  }
}
