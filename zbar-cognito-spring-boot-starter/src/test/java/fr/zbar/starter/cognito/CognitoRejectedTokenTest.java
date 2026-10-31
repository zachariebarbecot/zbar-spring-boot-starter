package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class CognitoRejectedTokenTest {

  @Test
  void should_return_401_when_token_is_missing() throws Exception {
    assertThat(status(issuer -> null)).isEqualTo(401);
  }

  @Test
  void should_return_401_when_token_is_expired() throws Exception {
    assertThat(
            statusWith(
                claims -> claims.expirationTime(Date.from(Instant.now().minusSeconds(3600)))))
        .isEqualTo(401);
  }

  @Test
  void should_return_401_when_issuer_is_wrong() throws Exception {
    assertThat(statusWith(claims -> claims.issuer("https://other.example.com/pool")))
        .isEqualTo(401);
  }

  @Test
  void should_return_401_when_client_id_is_not_allowed() throws Exception {
    assertThat(statusWith(claims -> claims.claim("client_id", "other-client"))).isEqualTo(401);
  }

  @Test
  void should_return_401_when_token_use_is_id() throws Exception {
    assertThat(statusWith(claims -> claims.claim("token_use", "id"))).isEqualTo(401);
  }

  private int statusWith(UnaryOperator<JWTClaimsSet.Builder> claims) throws Exception {
    return status(issuer -> issuer.token(claims));
  }

  private int status(Function<LocalIssuer, String> token) throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context = CognitoTestApplication.run(issuer)) {
      String bearer = token.apply(issuer);
      return CognitoTestApplication.client(context)
          .get()
          .uri("/hello")
          .headers(
              headers -> {
                if (bearer != null) {
                  headers.setBearerAuth(bearer);
                }
              })
          .exchange((request, response) -> response.getStatusCode().value());
    }
  }
}
