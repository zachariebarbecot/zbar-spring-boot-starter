package fr.zbar.starter.actuator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

final class ActuatorTestApplication {

  private ActuatorTestApplication() {}

  static ConfigurableApplicationContext run(String... args) {
    return new SpringApplicationBuilder(PlainApplication.class)
        .properties("server.port=0")
        .run(args);
  }

  static HttpResponse<String> get(ConfigurableApplicationContext context, String path)
      throws Exception {
    String port = context.getEnvironment().getProperty("local.server.port");
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
