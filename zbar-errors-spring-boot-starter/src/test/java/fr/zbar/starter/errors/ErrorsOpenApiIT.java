package fr.zbar.starter.errors;

import static fr.zbar.starter.errors.ErrorsTestSupport.client;
import static fr.zbar.starter.errors.ErrorsTestSupport.run;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class ErrorsOpenApiIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void should_publish_problem_detail_schema_when_springdoc_is_present() {
    Map<String, Object> schema = schema("ProblemDetail");

    assertThat(schema).containsKey("properties");
    assertThat(properties(schema))
        .containsOnlyKeys("type", "title", "status", "detail", "instance", "traceId", "errors");
    Map<String, Object> errors = child(properties(schema), "errors");
    assertThat(errors).containsEntry("type", "array");
    assertThat(properties(child(errors, "items"))).containsOnlyKeys("field", "message");
  }

  @Test
  void should_reference_problem_detail_in_error_responses_of_documented_endpoint() {
    Map<String, Object> responses = responses("/read-only");

    for (String status : List.of("400", "404", "500")) {
      Map<String, Object> content = child(child(responses, status), "content");
      assertThat(content).containsOnlyKeys("application/problem+json");
      assertThat(child(child(content, "application/problem+json"), "schema"))
          .containsEntry("$ref", "#/components/schemas/ProblemDetail");
    }
  }

  @Test
  void should_keep_application_declared_responses() {
    Map<String, Object> responses = responses("/documented");

    assertThat(child(responses, "400")).containsEntry("description", "custom bad request");
    assertThat(child(responses, "500")).containsEntry("description", "custom failure");
    assertThat(child(responses, "404")).extractingByKey("content").isInstanceOf(Map.class);
  }

  private Map<String, Object> responses(String path) {
    Map<String, Object> paths = child(apiDocs(), "paths");
    return child(child(child(paths, path), "get"), "responses");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> child(Map<String, Object> parent, String key) {
    return (Map<String, Object>) parent.getOrDefault(key, Map.of());
  }

  private static Map<String, Object> properties(Map<String, Object> schema) {
    return child(schema, "properties");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> schema(String name) {
    Map<String, Map<String, Object>> schemas =
        (Map<String, Map<String, Object>>)
            ((Map<String, Object>) apiDocs().getOrDefault("components", Map.of()))
                .getOrDefault("schemas", Map.of());
    return schemas.getOrDefault(name, Map.of());
  }

  private Map<String, Object> apiDocs() {
    try (ConfigurableApplicationContext context = run(Application.class)) {
      String body = client(context).get().uri("/v3/api-docs").retrieve().body(String.class);
      return JSON.readValue(body, new TypeReference<>() {});
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({SampleController.class, DocumentedController.class})
  static class Application {}
}
