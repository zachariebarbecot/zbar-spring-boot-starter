package fr.zbar.starter.persistence;

import java.util.UUID;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
class PersistenceTestApplication {

  static ConfigurableApplicationContext run(String... args) {
    return new SpringApplicationBuilder(PersistenceTestApplication.class)
        .web(WebApplicationType.NONE)
        .run(withDataSource(args));
  }

  static ConfigurableApplicationContext runWithoutDataSource() {
    return new SpringApplicationBuilder(PersistenceTestApplication.class)
        .web(WebApplicationType.NONE)
        .run(
            "--spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration");
  }

  private static String[] withDataSource(String[] args) {
    String[] all = new String[args.length + 2];
    all[0] = "--spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID();
    all[1] = "--spring.liquibase.enabled=false";
    System.arraycopy(args, 0, all, 2, args.length);
    return all;
  }
}
