package fr.zbar.starter.errors;

import static fr.zbar.starter.errors.ErrorsTestSupport.client;
import static fr.zbar.starter.errors.ErrorsTestSupport.run;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class ErrorsResponseIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void should_answer_problem_json_when_response_status_exception_is_thrown() {
    ResponseEntity<String> response = get("/conflict", null);

    assertProblem(response, 409, "Conflict");
  }

  @Test
  void should_answer_problem_json_when_resource_is_unknown() {
    ResponseEntity<String> response = get("/unknown", null);

    assertProblem(response, 404, "Not Found");
  }

  @Test
  void should_answer_problem_json_when_method_is_not_allowed() {
    ResponseEntity<String> response = send("/read-only", "POST", "{}", MediaType.APPLICATION_JSON);

    assertProblem(response, 405, "Method Not Allowed");
  }

  @Test
  void should_answer_problem_json_when_media_type_is_not_supported() {
    ResponseEntity<String> response = send("/json", "POST", "x", MediaType.TEXT_PLAIN);

    assertProblem(response, 415, "Unsupported Media Type");
  }

  @Test
  void should_answer_generic_detail_when_json_is_unreadable() {
    ResponseEntity<String> response =
        send("/json", "POST", "{\"secret-value\": oops", MediaType.APPLICATION_JSON);

    assertProblem(response, 400, "Bad Request");
    assertThat(response.getBody())
        .doesNotContain("secret-value", "oops", "Jackson", "Exception", "tools.jackson");
  }

  @Test
  void should_answer_problem_json_when_required_parameter_is_missing() {
    ResponseEntity<String> response = get("/param", null);

    assertProblem(response, 400, "Bad Request");
    assertThat(response.getBody()).doesNotContain("Exception", "org.springframework");
  }

  @Test
  void should_expose_allow_header_when_method_is_not_allowed() {
    ResponseEntity<String> response = send("/read-only", "POST", "{}", MediaType.APPLICATION_JSON);

    assertThat(response.getHeaders().getAllow()).isNotEmpty().contains(HttpMethod.GET);
  }

  @Test
  void should_keep_problem_json_when_client_only_accepts_json() {
    ResponseEntity<String> response = get("/unknown", MediaType.APPLICATION_JSON);

    assertProblem(response, 404, "Not Found");
  }

  private static void assertProblem(ResponseEntity<String> response, int status, String title) {
    assertThat(response.getStatusCode().value()).isEqualTo(status);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    Map<String, Object> body = JSON.readValue(response.getBody(), new TypeReference<>() {});
    assertThat(body).containsEntry("title", title).containsEntry("status", status);
    assertThat(body.getOrDefault("type", "about:blank")).isEqualTo("about:blank");
  }

  private ResponseEntity<String> get(String path, MediaType accept) {
    try (ConfigurableApplicationContext context = run(Application.class)) {
      RequestHeadersSpec<?> request = client(context).get().uri(path);
      if (accept != null) {
        request = request.accept(accept);
      }
      return request.retrieve().toEntity(String.class);
    }
  }

  private ResponseEntity<String> send(
      String path, String method, String body, MediaType contentType) {
    try (ConfigurableApplicationContext context = run(Application.class)) {
      return client(context)
          .method(HttpMethod.valueOf(method))
          .uri(path)
          .contentType(contentType)
          .body(body)
          .retrieve()
          .toEntity(String.class);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(SampleController.class)
  static class Application {}
}
