package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class CognitoAccessTokenTest {

  @Test
  void should_return_200_when_access_token_is_valid() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context = CognitoTestApplication.run(issuer)) {
      int status =
          CognitoTestApplication.client(context)
              .get()
              .uri("/hello")
              .headers(headers -> headers.setBearerAuth(issuer.accessToken()))
              .exchange((request, response) -> response.getStatusCode().value());

      assertThat(status).isEqualTo(200);
    }
  }
}
