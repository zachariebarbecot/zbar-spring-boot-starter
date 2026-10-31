package fr.zbar.starter.cognito;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@Configuration
@EnableAutoConfiguration
class CognitoTestApplication {

  @RestController
  static class HelloController {

    @GetMapping("/hello")
    String hello() {
      return "hello";
    }
  }

  static ConfigurableApplicationContext run(LocalIssuer issuer, String... properties) {
    return start(
            "zbar.cognito.issuer-uri=" + issuer.issuer(),
            "zbar.cognito.allowed-client-ids=" + LocalIssuer.CLIENT_ID)
        .properties(properties)
        .run();
  }

  static SpringApplicationBuilder start(String... properties) {
    return new SpringApplicationBuilder(CognitoTestApplication.class, HelloController.class)
        .web(WebApplicationType.SERVLET)
        .properties("server.port=0")
        .properties(properties);
  }

  static RestClient client(ConfigurableApplicationContext context) {
    String port = context.getEnvironment().getProperty("local.server.port");
    return RestClient.builder()
        .baseUrl("http://localhost:" + port)
        .defaultStatusHandler(status -> true, (request, response) -> {})
        .build();
  }
}
