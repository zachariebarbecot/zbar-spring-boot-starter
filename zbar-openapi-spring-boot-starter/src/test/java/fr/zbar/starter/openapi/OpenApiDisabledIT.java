package fr.zbar.starter.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class OpenApiDisabledIT {

  @Test
  void should_not_expose_api_docs_and_swagger_ui_when_disabled() throws Exception {
    try (ConfigurableApplicationContext context = run("zbar.openapi.enabled=false")) {
      assertThat(status(context, "/v3/api-docs")).isEqualTo(404);
      assertThat(status(context, "/swagger-ui/index.html")).isEqualTo(404);
    }
  }

  @Test
  void should_start_without_starter_beans_when_disabled() {
    try (ConfigurableApplicationContext context = run("zbar.openapi.enabled=false")) {
      assertThat(context.containsBean("zbarOpenApiCustomizer")).isFalse();
    }
  }

  @Test
  void should_expose_api_docs_when_enabled_is_absent_or_true() throws Exception {
    try (ConfigurableApplicationContext context = run()) {
      assertThat(status(context, "/v3/api-docs")).isEqualTo(200);
    }
    try (ConfigurableApplicationContext context = run("zbar.openapi.enabled=true")) {
      assertThat(status(context, "/v3/api-docs")).isEqualTo(200);
      assertThat(context.containsBean("zbarOpenApiCustomizer")).isTrue();
    }
  }

  @Test
  void should_let_explicit_springdoc_property_win_when_disabled() throws Exception {
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(PlainApplication.class)
            .properties("server.port=0", "zbar.openapi.enabled=false")
            .run("--springdoc.api-docs.enabled=true")) {
      assertThat(status(context, "/v3/api-docs")).isEqualTo(200);
    }
  }

  private ConfigurableApplicationContext run(String... properties) {
    return new SpringApplicationBuilder(PlainApplication.class)
        .properties("server.port=0")
        .properties(properties)
        .run();
  }

  private int status(ConfigurableApplicationContext context, String path) throws Exception {
    String port = context.getEnvironment().getProperty("local.server.port");
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
            HttpResponse.BodyHandlers.discarding())
        .statusCode();
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
