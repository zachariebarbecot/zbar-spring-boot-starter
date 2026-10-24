package fr.zbar.starter.logs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class LogsAutoConfigurationIT {

  @Test
  void should_activate_starter_when_enabled_is_absent() {
    try (ConfigurableApplicationContext context = run()) {
      assertThat(context.getBeansOfType(LogsAutoConfiguration.class)).hasSize(1);
      assertThat(context.getBeansOfType(LogsProperties.class)).hasSize(1);
    }
  }

  @Test
  void should_activate_starter_when_enabled_is_true() {
    try (ConfigurableApplicationContext context = run("zbar.logs.enabled=true")) {
      assertThat(context.getBeansOfType(LogsAutoConfiguration.class)).hasSize(1);
      assertThat(context.getBeansOfType(LogsProperties.class)).hasSize(1);
    }
  }

  @Test
  void should_create_no_starter_bean_when_enabled_is_false() {
    try (ConfigurableApplicationContext context = run("zbar.logs.enabled=false")) {
      assertThat(context.getBeansOfType(LogsAutoConfiguration.class)).isEmpty();
      assertThat(context.getBeansOfType(LogsProperties.class)).isEmpty();
    }
  }

  @Test
  void should_create_server_and_client_handlers_when_enabled_in_a_servlet_application() {
    try (ConfigurableApplicationContext context = runServlet()) {
      assertThat(context.getBeansOfType(RequestLoggingObservationHandler.class)).hasSize(1);
      assertThat(context.getBeansOfType(OutgoingRequestLoggingObservationHandler.class)).hasSize(1);
      assertThat(context.getBeansOfType(WebClientLoggingObservationHandler.class)).hasSize(1);
    }
  }

  @Test
  void should_create_no_handler_when_disabled_in_a_servlet_application() {
    try (ConfigurableApplicationContext context = runServlet("zbar.logs.enabled=false")) {
      assertThat(context.getBeansOfType(RequestLoggingObservationHandler.class)).isEmpty();
      assertThat(context.getBeansOfType(OutgoingRequestLoggingObservationHandler.class)).isEmpty();
      assertThat(context.getBeansOfType(WebClientLoggingObservationHandler.class)).isEmpty();
    }
  }

  private ConfigurableApplicationContext runServlet(String... properties) {
    return new SpringApplicationBuilder(PlainApplication.class)
        .web(WebApplicationType.SERVLET)
        .properties("server.port=0")
        .properties(properties)
        .run();
  }

  private ConfigurableApplicationContext run(String... properties) {
    return new SpringApplicationBuilder(PlainApplication.class)
        .web(WebApplicationType.NONE)
        .properties(properties)
        .run();
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
