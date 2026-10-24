package fr.zbar.starter.restclient;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

final class RestClientTestApplication {

  private RestClientTestApplication() {}

  static ConfigurableApplicationContext run(String... args) {
    return run(PlainApplication.class, args);
  }

  static ConfigurableApplicationContext run(Class<?> configuration, String... args) {
    return new SpringApplicationBuilder(configuration).web(WebApplicationType.NONE).run(args);
  }

  @Configuration
  @EnableAutoConfiguration
  static class PlainApplication {}
}
