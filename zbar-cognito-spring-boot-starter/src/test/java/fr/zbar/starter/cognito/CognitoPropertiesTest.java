package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CognitoPropertiesTest {

  @Test
  void should_use_issuer_uri_when_configured() {
    CognitoProperties properties =
        new CognitoProperties(
            true, "https://issuer.example.com/pool", "eu-west-1", "eu-west-1_abc", List.of("c"));

    assertThat(properties.issuer()).isEqualTo("https://issuer.example.com/pool");
  }

  @Test
  void should_derive_issuer_from_region_and_user_pool_when_issuer_uri_is_absent() {
    CognitoProperties properties =
        new CognitoProperties(true, null, "eu-west-1", "eu-west-1_abc", List.of("c"));

    assertThat(properties.issuer())
        .isEqualTo("https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_abc");
  }
}
