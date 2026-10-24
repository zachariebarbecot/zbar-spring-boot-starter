package fr.zbar.starter.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({OutputCaptureExtension.class, ClearStructuredFormatExtension.class})
class LogsFormatIT {

  private static final Logger LOGGER = LoggerFactory.getLogger("format.test");
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @TempDir Path tempDir;

  @Test
  void should_write_console_format_when_json_profile_is_inactive(CapturedOutput output) {
    run(null);
    LOGGER.info("hello-console");

    assertThat(lineContaining(output, "hello-console")).doesNotStartWith("{");
  }

  @Test
  void should_write_logstash_json_when_json_profile_is_active(CapturedOutput output) {
    run("json");
    LOGGER.info("hello-json");

    JsonNode json = MAPPER.readTree(lineContaining(output, "hello-json"));
    assertThat(json.get("@timestamp").asString()).isNotBlank();
    assertThat(json.get("level").asString()).isEqualTo("INFO");
    assertThat(json.get("logger_name").asString()).isEqualTo("format.test");
    assertThat(json.get("message").asString()).isEqualTo("hello-json");
  }

  @Test
  void should_write_stack_trace_in_same_json_object_when_exception_is_logged(
      CapturedOutput output) {
    run("json");
    LOGGER.error("boom-json", new IllegalStateException("kaput"));

    JsonNode json = MAPPER.readTree(lineContaining(output, "boom-json"));
    assertThat(json.get("stack_trace").asString()).contains("IllegalStateException: kaput");
  }

  @Test
  void should_use_user_format_when_console_format_is_defined(CapturedOutput output) {
    run("json", "logging.structured.format.console=ecs");
    LOGGER.info("hello-ecs");

    JsonNode json = MAPPER.readTree(lineContaining(output, "hello-ecs"));
    assertThat(json.has("ecs")).isTrue();
    assertThat(json.has("level")).isFalse();
  }

  @Test
  void should_write_console_format_when_disabled_with_json_profile(CapturedOutput output) {
    run("json", "zbar.logs.enabled=false");
    LOGGER.info("hello-disabled");

    assertThat(lineContaining(output, "hello-disabled")).doesNotStartWith("{");
  }

  @Test
  void should_write_application_version_and_environment_when_all_sources_are_set(
      CapturedOutput output) throws Exception {
    JsonNode json =
        fixedFields(
            output,
            "json",
            "spring.application.name=demo",
            gitProperties("git.commit.id.abbrev=abc1234"),
            "zbar.logs.environment=prod");

    assertThat(json.get("application").asString()).isEqualTo("demo");
    assertThat(json.get("version").asString()).isEqualTo("abc1234");
    assertThat(json.get("environment").asString()).isEqualTo("prod");
  }

  private JsonNode fixedFields(CapturedOutput output, String profiles, String... properties) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(PlainApplication.class)
            .web(WebApplicationType.NONE)
            .profiles(profiles.split(","))
            .properties(properties);
    try (ConfigurableApplicationContext context = builder.run()) {
      LOGGER.info("fixed-fields");
    }
    return MAPPER.readTree(lineContaining(output, "fixed-fields"));
  }

  private String gitProperties(String content) throws Exception {
    Path file = Files.writeString(tempDir.resolve("git.properties"), content);
    return "spring.info.git.location=" + file.toUri();
  }

  private static String lineContaining(CapturedOutput output, String text) {
    return output
        .getOut()
        .lines()
        .filter(line -> line.contains(text))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no log line containing " + text));
  }

  private void run(String profile, String... properties) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(PlainApplication.class)
            .web(WebApplicationType.NONE)
            .properties(properties);
    if (profile != null) {
      builder.profiles(profile);
    }
    try (ConfigurableApplicationContext context = builder.run()) {
      assertThat(context.isRunning()).isTrue();
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
