package fr.zbar.starter.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

@SpringBootTest(
    classes = OpenApiStarterIT.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiStarterIT {

  @LocalServerPort int port;

  @Autowired ApplicationContext context;

  @Test
  void should_serve_openapi_document_when_application_starts() throws Exception {
    HttpResponse<String> response = get("/v3/api-docs");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"openapi\"");
  }

  @Test
  void should_serve_swagger_ui_when_application_starts() throws Exception {
    HttpResponse<String> response = get("/swagger-ui/index.html");

    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  void should_provide_springdoc_ui_transitively_when_starter_is_the_only_dependency() {
    assertThat(ClassUtils.isPresent("org.springdoc.webmvc.ui.SwaggerConfig", null)).isTrue();
  }

  @Test
  void should_load_auto_configuration_when_application_has_no_explicit_import() {
    assertThat(context.getBeansOfType(OpenApiAutoConfiguration.class)).hasSize(1);
    assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
        .contains(OpenApiAutoConfiguration.class.getName());
  }

  private HttpResponse<String> get(String path) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Configuration
  @EnableAutoConfiguration
  static class TestApplication {}
}
