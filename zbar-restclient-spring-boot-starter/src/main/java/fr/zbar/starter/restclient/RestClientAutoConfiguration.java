package fr.zbar.starter.restclient;

import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

@ConditionalOnProperty(prefix = "zbar.restclient", name = "enabled", matchIfMissing = true)
@AutoConfiguration
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientAutoConfiguration {

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  RestClientCustomizer zbarRestClientCustomizer(
      RestClientProperties properties, Environment environment) {
    String applicationName = environment.getProperty("spring.application.name");
    return builder -> {
      if (StringUtils.hasText(applicationName)) {
        builder.defaultHeaders(
            headers -> headers.putIfAbsent(HttpHeaders.USER_AGENT, List.of(applicationName)));
      }
      if (StringUtils.hasText(properties.baseUrl())) {
        builder.baseUrl(properties.baseUrl());
      }
    };
  }
}
