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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class ErrorsValidationIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void should_list_one_error_per_invalid_field_when_body_is_invalid() {
    ResponseEntity<String> response = post("/signup", "{\"username\":\"\",\"password\":\"short\"}");

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(errors(response))
        .containsExactlyInAnyOrder(
            Map.of("field", "username", "message", "must not be blank"),
            Map.of("field", "password", "message", "size must be between 8 and 2147483647"));
  }

  @Test
  void should_not_expose_rejected_value_when_validation_fails() {
    ResponseEntity<String> response =
        post("/signup", "{\"username\":\"bob\",\"password\":\"hunter2\"}");

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).doesNotContain("hunter2");
  }

  @Test
  void should_name_parameter_when_request_parameter_constraint_is_violated() {
    ResponseEntity<String> response = get("/items?size=0");

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(errors(response)).extracting(error -> error.get("field")).containsExactly("size");
  }

  @Test
  void should_name_parameter_when_class_level_constraint_is_violated() {
    ResponseEntity<String> response = get("/validated?page=0");

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(errors(response)).extracting(error -> error.get("field")).containsExactly("page");
  }

  @Test
  void should_name_parameter_when_path_variable_has_wrong_type() {
    ResponseEntity<String> response = get("/items/abc");

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(errors(response)).extracting(error -> error.get("field")).containsExactly("id");
    assertThat(response.getBody()).doesNotContain("NumberFormat", "For input");
  }

  @Test
  void should_use_object_name_as_field_when_violation_is_global() {
    ResponseEntity<String> response = post("/range", "{\"from\":5,\"to\":1}");

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(errors(response))
        .containsExactly(Map.of("field", "range", "message", "from must not exceed to"));
  }

  private static List<Map<String, String>> errors(ResponseEntity<String> response) {
    Map<String, Object> body = JSON.readValue(response.getBody(), new TypeReference<>() {});
    return JSON.convertValue(body.get("errors"), new TypeReference<>() {});
  }

  private ResponseEntity<String> get(String path) {
    try (ConfigurableApplicationContext context = run(Application.class)) {
      return client(context)
          .get()
          .uri(path)
          .header("Accept-Language", "en")
          .retrieve()
          .toEntity(String.class);
    }
  }

  private ResponseEntity<String> post(String path, String json) {
    try (ConfigurableApplicationContext context = run(Application.class)) {
      return client(context)
          .post()
          .uri(path)
          .header("Accept-Language", "en")
          .contentType(MediaType.APPLICATION_JSON)
          .body(json)
          .retrieve()
          .toEntity(String.class);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({ValidationController.class, ValidatedController.class})
  static class Application {}
}
