package fr.zbar.starter.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class OpenApiTitleIT {

  @Test
  void should_use_application_name_as_title_when_name_is_set() throws Exception {
    String title = title(PlainApplication.class, "spring.application.name=demo");

    assertThat(title).isEqualTo("demo");
  }

  @Test
  void should_keep_default_title_when_application_name_is_absent() throws Exception {
    String title = title(PlainApplication.class);

    assertThat(title).isEqualTo("OpenAPI definition");
  }

  @Test
  void should_set_title_and_keep_user_bean_when_user_openapi_has_no_title() throws Exception {
    String body = apiDocs(UntitledApplication.class, "spring.application.name=demo");

    assertThat(JsonPath.<String>read(body, "$.info.title")).isEqualTo("demo");
    assertThat(JsonPath.<String>read(body, "$.info.description")).isEqualTo("custom description");
  }

  @Test
  void should_keep_user_title_when_user_openapi_has_explicit_title() throws Exception {
    String title = title(TitledApplication.class, "spring.application.name=demo");

    assertThat(title).isEqualTo("My API");
  }

  private String title(Class<?> application, String... properties) throws Exception {
    return JsonPath.read(apiDocs(application, properties), "$.info.title");
  }

  private String apiDocs(Class<?> application, String... properties) throws Exception {
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(application)
            .properties("server.port=0")
            .properties(properties)
            .run()) {
      String port = context.getEnvironment().getProperty("local.server.port");
      return HttpClient.newHttpClient()
          .send(
              HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                  .build(),
              HttpResponse.BodyHandlers.ofString())
          .body();
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}

  @Configuration
  @EnableAutoConfiguration
  static class UntitledApplication {
    @Bean
    OpenAPI openApi() {
      return new OpenAPI().info(new Info().description("custom description"));
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class TitledApplication {
    @Bean
    OpenAPI openApi() {
      return new OpenAPI().info(new Info().title("My API"));
    }
  }
}
