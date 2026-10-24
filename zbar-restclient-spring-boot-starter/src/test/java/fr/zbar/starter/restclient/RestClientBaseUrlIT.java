package fr.zbar.starter.restclient;

import static fr.zbar.starter.restclient.RecordingServer.call;
import static fr.zbar.starter.restclient.RestClientTestApplication.run;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class RestClientBaseUrlIT {

  private RecordingServer server;

  @BeforeEach
  void startServer() throws IOException {
    server = new RecordingServer();
  }

  @AfterEach
  void stopServer() {
    server.close();
  }

  @Test
  void should_resolve_relative_uri_against_base_url() {
    try (ConfigurableApplicationContext context =
        run("--zbar.restclient.base-url=" + server.baseUrl())) {
      call(context, "/ping");

      assertThat(server.lastPath()).isEqualTo("/ping");
    }
  }

  @Test
  void should_call_absolute_url_as_is_when_base_url_is_absent() {
    try (ConfigurableApplicationContext context = run()) {
      call(context, server.baseUrl() + "/absolute");

      assertThat(server.lastPath()).isEqualTo("/absolute");
    }
  }
}
