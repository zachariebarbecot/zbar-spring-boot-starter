package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class CognitoPublicRoutesTest {

  @Test
  void should_not_require_token_on_default_health_and_documentation_paths() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context = CognitoTestApplication.run(issuer)) {
      assertThat(status(context, "/actuator/health")).isNotEqualTo(401);
      assertThat(status(context, "/actuator/health/liveness")).isNotEqualTo(401);
      assertThat(status(context, "/v3/api-docs")).isNotEqualTo(401);
      assertThat(status(context, "/v3/api-docs/swagger-config")).isNotEqualTo(401);
      assertThat(status(context, "/swagger-ui/index.html")).isNotEqualTo(401);
      assertThat(status(context, "/swagger-ui.html")).isNotEqualTo(401);
    }
  }

  @Test
  void should_not_require_token_on_configured_health_and_documentation_paths() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context =
            CognitoTestApplication.run(
                issuer,
                "management.endpoints.web.base-path=/manage",
                "springdoc.api-docs.path=/docs",
                "springdoc.swagger-ui.path=/ui")) {
      assertThat(status(context, "/manage/health")).isNotEqualTo(401);
      assertThat(status(context, "/docs")).isNotEqualTo(401);
      assertThat(status(context, "/ui")).isNotEqualTo(401);
      assertThat(status(context, "/actuator/health")).isEqualTo(401);
      assertThat(status(context, "/v3/api-docs")).isEqualTo(401);
    }
  }

  @Test
  void should_still_require_token_on_application_endpoint() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context = CognitoTestApplication.run(issuer)) {
      assertThat(status(context, "/hello")).isEqualTo(401);
    }
  }

  private int status(ConfigurableApplicationContext context, String path) {
    return CognitoTestApplication.client(context)
        .get()
        .uri(path)
        .exchange((request, response) -> response.getStatusCode().value());
  }
}
