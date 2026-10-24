package fr.zbar.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

class RestClientHeaderPropagationIT {

  private RecordingServer server;

  @BeforeEach
  void startServer() throws IOException {
    server = new RecordingServer();
  }

  @AfterEach
  void stopServer() {
    server.close();
  }

  private ConfigurableApplicationContext runWeb(String... args) {
    String[] all = new String[args.length + 1];
    all[0] = "--server.port=0";
    System.arraycopy(args, 0, all, 1, args.length);
    return new SpringApplicationBuilder(RelayApplication.class)
        .web(WebApplicationType.SERVLET)
        .run(all);
  }

  /** Sends an incoming request carrying the given headers; the app relays it to the server. */
  private void relay(ConfigurableApplicationContext context, String... headers) throws Exception {
    String port = context.getEnvironment().getProperty("local.server.port");
    HttpRequest.Builder request =
        HttpRequest.newBuilder(
            URI.create("http://localhost:" + port + "/relay?target=" + server.baseUrl() + "/ping"));
    for (int i = 0; i < headers.length; i += 2) {
      request.header(headers[i], headers[i + 1]);
    }
    HttpClient.newHttpClient().send(request.build(), BodyHandlers.discarding());
  }

  @Test
  void should_forward_default_correlation_and_request_ids() throws Exception {
    try (ConfigurableApplicationContext context = runWeb()) {
      relay(
          context,
          "X-Correlation-Id",
          "corr-1",
          "X-Request-Id",
          "req-1",
          "X-Other",
          "not-forwarded");

      assertThat(server.lastHeaders().get("X-Correlation-Id")).containsExactly("corr-1");
      assertThat(server.lastHeaders().get("X-Request-Id")).containsExactly("req-1");
      assertThat(server.lastHeaders().get("X-Other")).isNull();
    }
  }

  @Test
  void should_forward_configured_headers_in_addition_to_defaults() throws Exception {
    try (ConfigurableApplicationContext context =
        runWeb("--zbar.restclient.propagated-headers=X-Tenant")) {
      relay(context, "X-Tenant", "acme", "X-Correlation-Id", "corr-1");

      assertThat(server.lastHeaders().get("X-Tenant")).containsExactly("acme");
      assertThat(server.lastHeaders().get("X-Correlation-Id")).containsExactly("corr-1");
    }
  }

  @Test
  void should_never_forward_credentials_even_when_configured() throws Exception {
    try (ConfigurableApplicationContext context =
        runWeb(
            "--zbar.restclient.propagated-headers=authorization,COOKIE,Proxy-Authorization,X-Tenant")) {
      relay(
          context,
          "Authorization",
          "Bearer secret",
          "Cookie",
          "session=secret",
          "Proxy-Authorization",
          "Basic secret",
          "X-Tenant",
          "acme");

      assertThat(server.lastHeaders().get("X-Tenant")).containsExactly("acme");
      assertThat(server.lastHeaders().get("Authorization")).isNull();
      assertThat(server.lastHeaders().get("Cookie")).isNull();
      assertThat(server.lastHeaders().get("Proxy-Authorization")).isNull();
    }
  }

  @Test
  void should_keep_header_set_by_application_whatever_its_case() throws Exception {
    try (ConfigurableApplicationContext context = runWeb()) {
      relay(context, "X-Correlation-Id", "incoming", "X-Override", "x-correlation-id=app-value");

      assertThat(server.lastHeaders().get("X-Correlation-Id")).containsExactly("app-value");
    }
  }

  @Test
  void should_add_no_header_and_not_fail_outside_an_incoming_request() {
    try (ConfigurableApplicationContext context = runWeb()) {
      RecordingServer.call(context, server.baseUrl() + "/ping");

      assertThat(server.lastPath()).isEqualTo("/ping");
      assertThat(server.lastHeaders().get("X-Correlation-Id")).isNull();
      assertThat(server.lastHeaders().get("X-Request-Id")).isNull();
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(RelayController.class)
  static class RelayApplication {}

  @RestController
  static class RelayController {

    private final RestClient.Builder builder;

    RelayController(RestClient.Builder builder) {
      this.builder = builder;
    }

    @GetMapping("/relay")
    void relay(
        @RequestParam String target,
        @RequestHeader(name = "X-Override", required = false) String override) {
      RestClient.RequestHeadersSpec<?> request = builder.build().get().uri(target);
      if (override != null) {
        String[] nameAndValue = override.split("=");
        request.header(nameAndValue[0], nameAndValue[1]);
      }
      request.retrieve().toBodilessEntity();
    }
  }
}
