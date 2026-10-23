package fr.zbar.starter.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class OpenApiVersionIT {

  @TempDir Path tempDir;

  @Test
  void should_use_abbreviated_commit_as_version_when_git_properties_has_it() throws Exception {
    String version = version(PlainApplication.class, gitProperties("git.commit.id.abbrev=a1b2c3d"));

    assertThat(version).isEqualTo("a1b2c3d");
  }

  @Test
  void should_keep_default_version_when_git_properties_is_absent() throws Exception {
    String version =
        version(
            PlainApplication.class,
            "spring.info.git.location=" + tempDir.resolve("missing.properties").toUri());

    assertThat(version).isEqualTo("v0");
  }

  @Test
  void should_keep_default_version_when_git_properties_has_no_abbreviated_commit()
      throws Exception {
    String version = version(PlainApplication.class, gitProperties("git.branch=main"));

    assertThat(version).isEqualTo("v0");
  }

  @Test
  void should_keep_user_version_when_user_openapi_has_explicit_version() throws Exception {
    String version =
        version(VersionedApplication.class, gitProperties("git.commit.id.abbrev=a1b2c3d"));

    assertThat(version).isEqualTo("2.0.0");
  }

  private String gitProperties(String content) throws Exception {
    Path file = Files.writeString(tempDir.resolve("git.properties"), content);
    return "spring.info.git.location=" + file.toUri();
  }

  private String version(Class<?> application, String... properties) throws Exception {
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(application)
            .properties("server.port=0")
            .properties(properties)
            .run()) {
      String port = context.getEnvironment().getProperty("local.server.port");
      String body =
          HttpClient.newHttpClient()
              .send(
                  HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                      .build(),
                  HttpResponse.BodyHandlers.ofString())
              .body();
      return JsonPath.read(body, "$.info.version");
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}

  @Configuration
  @EnableAutoConfiguration
  static class VersionedApplication {
    @Bean
    OpenAPI openApi() {
      return new OpenAPI().info(new Info().version("2.0.0"));
    }
  }
}
