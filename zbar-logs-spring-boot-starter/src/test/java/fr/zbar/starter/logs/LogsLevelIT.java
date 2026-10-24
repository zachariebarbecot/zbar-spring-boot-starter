package fr.zbar.starter.logs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@ExtendWith(OutputCaptureExtension.class)
class LogsLevelIT {

  @AfterEach
  void resetLogging() {
    System.clearProperty("CONSOLE_LOG_STRUCTURED_FORMAT");
  }

  @Test
  void should_hide_info_logs_of_a_package_set_to_warn_in_console(CapturedOutput output) {
    run();

    assertThat(output.getOut())
        .contains("other-package-info")
        .doesNotContain("acme-info")
        .contains("acme-warn");
  }

  @Test
  void should_hide_info_logs_of_a_package_set_to_warn_in_json(CapturedOutput output) {
    run("logging.structured.format.console=logstash");

    assertThat(output.getOut())
        .contains("\"message\":\"other-package-info\"")
        .contains("\"message\":\"acme-warn\"")
        .doesNotContain("acme-info");
  }

  private void run(String... properties) {
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(App.class)
            .web(WebApplicationType.NONE)
            .properties("logging.level.com.acme=WARN")
            .properties(properties)
            .run()) {
      LoggerFactory.getLogger("com.acme.Probe").info("acme-info");
      LoggerFactory.getLogger("com.acme.Probe").warn("acme-warn");
      LoggerFactory.getLogger("org.other.Probe").info("other-package-info");
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class App {}
}
