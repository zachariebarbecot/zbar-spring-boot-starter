package fr.zbar.starter.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;
import org.testcontainers.mockserver.MockServerContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** One container per image and reuse setting for the whole JVM, whatever the number of contexts. */
final class ZbarContainers {

  private record Key(String image, boolean reuse) {}

  private static final Map<Key, PostgreSQLContainer> POSTGRES = new ConcurrentHashMap<>();
  private static final Map<Key, MockServerContainer> MOCK_SERVERS = new ConcurrentHashMap<>();

  private ZbarContainers() {}

  static boolean reuse(Environment environment) {
    return environment.getProperty("zbar.test.reuse", Boolean.class, false);
  }

  static PostgreSQLContainer postgres(String image, boolean reuse) {
    return POSTGRES.computeIfAbsent(
        new Key(image, reuse), key -> new SharedPostgres(image).withReuse(reuse));
  }

  static MockServerContainer mockServer(String image, boolean reuse) {
    return MOCK_SERVERS.computeIfAbsent(
        new Key(image, reuse),
        key -> new SharedMockServer(DockerImageName.parse(image)).withReuse(reuse));
  }

  /**
   * Spring closes container beans with their context: the JVM-wide container ignores it (Ryuk
   * cleans up).
   */
  private static final class SharedPostgres extends PostgreSQLContainer {

    SharedPostgres(String image) {
      super(image);
    }

    @Override
    public void stop() {}
  }

  private static final class SharedMockServer extends MockServerContainer {

    SharedMockServer(DockerImageName image) {
      super(image);
    }

    @Override
    public void stop() {}
  }
}
