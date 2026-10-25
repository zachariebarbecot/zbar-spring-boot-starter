package fr.zbar.starter.persistence;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;

@Configuration
@EnableAutoConfiguration
class PersistenceTestApplication {

  static ConfigurableApplicationContext run(String... args) {
    return new SpringApplicationBuilder(PersistenceTestApplication.class)
        .web(WebApplicationType.NONE)
        .run(withDataSource(args));
  }

  static ConfigurableApplicationContext runWithoutDefaultChangelog() {
    return new SpringApplicationBuilder(PersistenceTestApplication.class)
        .web(WebApplicationType.NONE)
        .resourceLoader(new DefaultResourceLoader(new ClassLoaderWithoutChangelog()))
        .run(withDataSource(new String[0]));
  }

  static ConfigurableApplicationContext runWithoutDataSource() {
    return new SpringApplicationBuilder(PersistenceTestApplication.class)
        .web(WebApplicationType.NONE)
        .run(
            "--spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration");
  }

  private static final class ClassLoaderWithoutChangelog extends ClassLoader {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    ClassLoaderWithoutChangelog() {
      super(PersistenceTestApplication.class.getClassLoader());
    }

    @Override
    public URL getResource(String name) {
      return CHANGELOG.equals(name) ? null : super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
      return CHANGELOG.equals(name) ? Collections.emptyEnumeration() : super.getResources(name);
    }
  }

  private static String[] withDataSource(String[] args) {
    String[] all = new String[args.length + 1];
    all[0] = "--spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID();
    System.arraycopy(args, 0, all, 1, args.length);
    return all;
  }
}
