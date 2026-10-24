package fr.zbar.starter.transaction;

import java.util.UUID;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableAutoConfiguration
class TransactionsTestApplication {

  static ConfigurableApplicationContext run() {
    ConfigurableApplicationContext context =
        new SpringApplicationBuilder(TransactionsTestApplication.class)
            .web(WebApplicationType.NONE)
            .properties(
                "spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
            .run();
    context.getBean(JdbcTemplate.class).execute("create table item(name varchar(50))");
    return context;
  }
}
