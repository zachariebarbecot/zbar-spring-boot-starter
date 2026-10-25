package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.zbar.starter.test.ZbarPostgresTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

@ZbarPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
    classes = PersistenceApplication.class,
    properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.liquibase.enabled=false"})
class PersistencePostgresIT {

  @Autowired EntityManager entityManager;
  @Autowired TransactionTemplate transactions;
  @Autowired JdbcTemplate jdbc;

  @Test
  void should_map_camel_case_to_snake_case_and_keep_instant_in_utc() {
    Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    AuditedEntity entity = new AuditedEntity();
    entity.createdAt = createdAt;
    transactions.executeWithoutResult(status -> entityManager.persist(entity));

    Instant reloaded =
        transactions.execute(
            status -> entityManager.find(AuditedEntity.class, entity.id).createdAt);

    assertThat(reloaded).isEqualTo(createdAt);
    List<String> columns =
        jdbc.queryForList(
            "select column_name from information_schema.columns where table_name = 'audited_entity'",
            String.class);
    assertThat(columns).contains("created_at");
    assertThat(
            jdbc.queryForObject(
                "select created_at at time zone 'UTC' from audited_entity", LocalDateTime.class))
        .isEqualTo(LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC));
  }
}
