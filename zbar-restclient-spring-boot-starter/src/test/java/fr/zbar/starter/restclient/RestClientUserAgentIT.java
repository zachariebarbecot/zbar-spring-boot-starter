package fr.zbar.starter.restclient;

import static fr.zbar.starter.restclient.RecordingServer.call;
import static fr.zbar.starter.restclient.RestClientTestApplication.run;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

class RestClientUserAgentIT {

  private RecordingServer server;

  @BeforeEach
  void startServer() throws IOException {
    server = new RecordingServer();
  }

  @AfterEach
  void stopServer() {
    server.close();
  }

  private List<String> userAgents() {
    return server.lastHeaders().get(HttpHeaders.USER_AGENT);
  }

  private String url() {
    return server.baseUrl() + "/ping";
  }

  @Test
  void should_send_application_name_as_user_agent() {
    try (ConfigurableApplicationContext context = run("--spring.application.name=orders")) {
      call(context, url());

      assertThat(userAgents()).containsExactly("orders");
    }
  }

  @Test
  void should_not_add_user_agent_when_application_name_is_absent() {
    try (ConfigurableApplicationContext context = run()) {
      call(context, url());

      assertThat(userAgents()).isEqualTo(defaultUserAgents());
    }
  }

  private List<String> defaultUserAgents() {
    try (ConfigurableApplicationContext context =
        run("--zbar.restclient.enabled=false", "--spring.application.name=orders")) {
      call(context, url());
      return userAgents();
    }
  }

  @Test
  void should_keep_user_agent_set_on_the_request() {
    try (ConfigurableApplicationContext context = run("--spring.application.name=orders")) {
      context
          .getBean(RestClient.Builder.class)
          .build()
          .get()
          .uri(url())
          .header(HttpHeaders.USER_AGENT, "billing-batch")
          .retrieve()
          .toBodilessEntity();

      assertThat(userAgents()).containsExactly("billing-batch");
    }
  }

  @Test
  void should_keep_user_agent_set_by_application_customizer() {
    try (ConfigurableApplicationContext context =
        run(CustomizedApplication.class, "--spring.application.name=orders")) {
      call(context, url());

      assertThat(userAgents()).containsExactly("billing-batch");
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class CustomizedApplication {

    @Bean
    RestClientCustomizer userAgentCustomizer() {
      return builder -> builder.defaultHeader(HttpHeaders.USER_AGENT, "billing-batch");
    }
  }
}
