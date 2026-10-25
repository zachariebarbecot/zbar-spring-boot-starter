package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.zbar.starter.test.ZbarPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@ZbarPostgresTest
@SpringBootTest(classes = PersistenceApplication.class)
class PersistenceLiquibasePostgresIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired Environment environment;

  @Test
  void should_apply_changelog_and_pass_hibernate_validation() {
    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    assertThat(
            jdbc.queryForList(
                "select table_name from information_schema.tables where table_name = 'audited_entity'",
                String.class))
        .containsExactly("audited_entity");
    assertThat(jdbc.queryForObject("select count(*) from databasechangelog", Integer.class))
        .isEqualTo(2);
  }
}
