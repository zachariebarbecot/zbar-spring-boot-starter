package fr.zbar.starter.restclient;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.StringUtils;

@ConditionalOnProperty(prefix = "zbar.restclient", name = "enabled", matchIfMissing = true)
@AutoConfiguration
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientAutoConfiguration {

  private static final JacksonJsonHttpMessageConverter PROBLEM_CONVERTER =
      new JacksonJsonHttpMessageConverter();

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

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  RestClientCustomizer zbarErrorHandlingCustomizer() {
    return builder ->
        builder
            .defaultStatusHandler(
                HttpStatusCode::is4xxClientError,
                (request, response) -> {
                  throw new RestClientClientErrorException(
                      response.getStatusCode(),
                      request.getMethod(),
                      request.getURI(),
                      problemDetail(response));
                })
            .defaultStatusHandler(
                HttpStatusCode::is5xxServerError,
                (request, response) -> {
                  throw new RestClientServerErrorException(
                      response.getStatusCode(),
                      request.getMethod(),
                      request.getURI(),
                      problemDetail(response));
                });
  }

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  @ConditionalOnWebApplication(type = Type.SERVLET)
  RestClientCustomizer zbarHeaderPropagationCustomizer(RestClientProperties properties) {
    IncomingHeadersInterceptor interceptor =
        new IncomingHeadersInterceptor(properties.propagatedHeaders());
    return builder -> builder.requestInterceptor(interceptor);
  }

  private static ProblemDetail problemDetail(ClientHttpResponse response) {
    if (!MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(
        response.getHeaders().getContentType())) {
      return null;
    }
    try {
      return (ProblemDetail) PROBLEM_CONVERTER.read(ProblemDetail.class, response);
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }
}
