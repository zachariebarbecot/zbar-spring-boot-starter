package fr.zbar.starter.restclient;

import static fr.zbar.starter.restclient.RecordingServer.call;
import static fr.zbar.starter.restclient.RestClientTestApplication.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

class RestClientErrorsIT {

  private RecordingServer server;

  @BeforeEach
  void startServer() throws IOException {
    server = new RecordingServer();
  }

  @AfterEach
  void stopServer() {
    server.close();
  }

  private Throwable failedCall(String path) {
    return failedCallTo(server.baseUrl() + path);
  }

  private Throwable failedCallTo(String url) {
    try (ConfigurableApplicationContext context = run()) {
      return catchThrowable(() -> call(context, url));
    }
  }

  @Test
  void should_throw_client_error_exception_with_status_method_and_url_on_4xx() {
    server.respondWith(404, "text/plain", "");

    Throwable thrown = failedCall("/orders/42");

    assertThat(thrown)
        .isInstanceOf(RestClientClientErrorException.class)
        .isInstanceOf(RestClientException.class);
    RestClientClientErrorException error = (RestClientClientErrorException) thrown;
    assertThat(error.statusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(error.method()).isEqualTo(HttpMethod.GET);
    assertThat(error.url()).hasToString(server.baseUrl() + "/orders/42");
  }

  @Test
  void should_throw_server_error_exception_distinct_from_client_error_on_5xx() {
    server.respondWith(503, "text/plain", "");

    Throwable thrown = failedCall("/orders/42");

    assertThat(thrown)
        .isInstanceOf(RestClientServerErrorException.class)
        .isNotInstanceOf(RestClientClientErrorException.class);
    RestClientServerErrorException error = (RestClientServerErrorException) thrown;
    assertThat(error.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(error.method()).isEqualTo(HttpMethod.GET);
    assertThat(error.url()).hasToString(server.baseUrl() + "/orders/42");
  }

  private static final String PROBLEM =
      """
      {"type":"https://errors.example/out-of-stock","title":"Out of stock","status":%d,\
      "detail":"Item 42 is unavailable"}\
      """;

  @ParameterizedTest
  @ValueSource(ints = {409, 502})
  void should_expose_problem_detail_when_error_is_problem_json(int status) {
    server.respondWith(status, "application/problem+json", PROBLEM.formatted(status));

    RestClientStatusException error = (RestClientStatusException) failedCall("/orders/42");

    assertThat(error.problemDetail()).isNotNull();
    assertThat(error.problemDetail().getTitle()).isEqualTo("Out of stock");
    assertThat(error.problemDetail().getDetail()).isEqualTo("Item 42 is unavailable");
    assertThat(error.problemDetail().getStatus()).isEqualTo(status);
  }

  @Test
  void should_have_no_problem_detail_when_content_type_is_not_problem_json() {
    server.respondWith(409, "application/json", PROBLEM.formatted(409));

    Throwable thrown = failedCall("/orders/42");

    assertThat(thrown).isInstanceOf(RestClientClientErrorException.class);
    assertThat(((RestClientStatusException) thrown).problemDetail()).isNull();
  }

  @Test
  void should_have_no_problem_detail_when_problem_json_body_is_not_parsable() {
    server.respondWith(502, "application/problem+json", "<html>bad gateway</html>");

    Throwable thrown = failedCall("/orders/42");

    assertThat(thrown).isInstanceOf(RestClientServerErrorException.class);
    assertThat(((RestClientStatusException) thrown).problemDetail()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"text/plain", "application/problem+json"})
  void should_mask_query_and_leave_raw_body_out_of_message(String contentType) {
    server.respondWith(500, contentType, "{\"title\":\"boom\",\"detail\":\"card 4111 refused\"}");

    Throwable thrown = failedCall("/orders/42?token=secret");

    assertThat(thrown.getMessage())
        .contains("/orders/42")
        .doesNotContain("token")
        .doesNotContain("secret")
        .doesNotContain("4111")
        .doesNotContain("boom");
  }

  @Test
  void should_leave_user_info_and_fragment_out_of_message() {
    server.respondWith(500, "text/plain", "");

    Throwable thrown =
        failedCallTo(
            server.baseUrl().replace("//", "//admin:hunter2@") + "/orders/42?token=secret#frag");

    assertThat(thrown.getMessage())
        .contains("/orders/42")
        .doesNotContain("admin")
        .doesNotContain("hunter2")
        .doesNotContain("frag");
  }
}
