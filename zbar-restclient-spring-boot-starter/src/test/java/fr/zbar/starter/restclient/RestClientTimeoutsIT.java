package fr.zbar.starter.restclient;

import static fr.zbar.starter.restclient.RestClientTestApplication.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;

class RestClientTimeoutsIT {

  @Test
  void should_default_connect_timeout_to_2s_and_read_timeout_to_10s() {
    try (ConfigurableApplicationContext context = run()) {
      assertThat(context.getEnvironment().getProperty("spring.http.clients.connect-timeout"))
          .isEqualTo("2s");
      assertThat(context.getEnvironment().getProperty("spring.http.clients.read-timeout"))
          .isEqualTo("10s");
    }
  }

  @Test
  void should_keep_application_timeouts_when_defined() {
    try (ConfigurableApplicationContext context =
        run(
            "--spring.http.clients.connect-timeout=3s",
            "--spring.http.clients.read-timeout=300ms")) {
      assertThat(context.getEnvironment().getProperty("spring.http.clients.connect-timeout"))
          .isEqualTo("3s");
      assertThat(context.getEnvironment().getProperty("spring.http.clients.read-timeout"))
          .isEqualTo("300ms");
    }
  }

  @Test
  void should_fail_on_read_timeout_when_server_answers_later_than_application_read_timeout()
      throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/slow",
        exchange -> {
          try {
            Thread.sleep(1500);
            exchange.sendResponseHeaders(200, -1);
          } catch (Exception ignored) {
            // client gave up
          } finally {
            exchange.close();
          }
        });
    server.start();
    try (ConfigurableApplicationContext context = run("--spring.http.clients.read-timeout=300ms")) {
      RestClient client = context.getBean(RestClient.Builder.class).build();
      String url = "http://localhost:" + server.getAddress().getPort() + "/slow";

      assertThatThrownBy(() -> client.get().uri(url).retrieve().toBodilessEntity())
          .hasRootCauseInstanceOf(HttpTimeoutException.class);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void should_not_add_defaults_nor_beans_when_disabled() {
    try (ConfigurableApplicationContext context = run("--zbar.restclient.enabled=false")) {
      assertThat(context.getEnvironment().getProperty("spring.http.clients.connect-timeout"))
          .isNull();
      assertThat(context.getEnvironment().getProperty("spring.http.clients.read-timeout")).isNull();
      assertThat(context.getBeansOfType(RestClientProperties.class)).isEmpty();
    }
  }
}
