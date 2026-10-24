package fr.zbar.starter.errors;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;

final class ErrorsTestSupport {

  private ErrorsTestSupport() {}

  static ConfigurableApplicationContext run(Class<?> application, String... properties) {
    return new SpringApplicationBuilder(application)
        .web(WebApplicationType.SERVLET)
        .properties("server.port=0")
        .properties(properties)
        .run();
  }

  static RestClient client(ConfigurableApplicationContext context) {
    String port = context.getEnvironment().getProperty("local.server.port");
    return RestClient.builder()
        .baseUrl("http://localhost:" + port)
        .defaultStatusHandler(status -> true, (request, response) -> {})
        .build();
  }
}
