package fr.zbar.starter.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class ActuatorInfoGitIT {

  @TempDir Path tempDir;

  @Test
  void should_show_branch_and_commit_in_info() throws Exception {
    try (ConfigurableApplicationContext context = run(gitProperties())) {
      String body = get(context, "/actuator/info").body();

      assertThat((String) JsonPath.read(body, "$.git.branch")).isEqualTo("main");
      assertThat((String) JsonPath.read(body, "$.git.commit.id")).isEqualTo("abc1234");
    }
  }

  @Test
  void should_start_and_show_no_git_block_when_git_properties_is_missing() throws Exception {
    try (ConfigurableApplicationContext context = run(tempDir.resolve("missing.properties"))) {
      HttpResponse<String> info = get(context, "/actuator/info");

      assertThat(info.statusCode()).isEqualTo(200);
      assertThat(info.body()).doesNotContain("\"git\"");
    }
  }

  @Test
  void should_hide_full_commit_message_and_build_user_email_by_default() throws Exception {
    try (ConfigurableApplicationContext context = run(gitProperties())) {
      String body = get(context, "/actuator/info").body();

      assertThat((String) JsonPath.read(body, "$.git.branch")).isEqualTo("main");
      assertThat(JsonPath.<Map<String, Object>>read(body, "$.git.commit"))
          .doesNotContainKey("message");
      assertThat(JsonPath.<Map<String, Object>>read(body, "$.git")).doesNotContainKey("build");
    }
  }

  @Test
  void should_let_application_git_mode_win() throws Exception {
    try (ConfigurableApplicationContext context =
        run(gitProperties(), "--management.info.git.mode=full")) {
      String body = get(context, "/actuator/info").body();

      assertThat((String) JsonPath.read(body, "$.git.commit.message.full"))
          .isEqualTo("initial commit");
    }
  }

  private Path gitProperties() throws Exception {
    return Files.writeString(
        tempDir.resolve("git.properties"),
        """
        git.branch=main
        git.commit.id=abc1234
        git.commit.id.abbrev=abc1234
        git.commit.time=2026-01-01T12:00:00Z
        git.commit.message.full=initial commit
        git.build.user.email=dev@example.com
        """);
  }

  private ConfigurableApplicationContext run(Path location, String... args) {
    return new SpringApplicationBuilder(PlainApplication.class)
        .properties("server.port=0")
        .run(
            Stream.concat(
                    Stream.of("--spring.info.git.location=" + location.toUri()), Stream.of(args))
                .toArray(String[]::new));
  }

  private HttpResponse<String> get(ConfigurableApplicationContext context, String path)
      throws Exception {
    String port = context.getEnvironment().getProperty("local.server.port");
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
