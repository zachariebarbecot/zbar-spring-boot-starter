package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class CognitoGroupsTest {

  @Test
  void should_map_groups_to_roles_keeping_case() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context = start(issuer)) {
      String body =
          authorities(
              context,
              issuer.token(claims -> claims.claim("cognito:groups", List.of("Admin", "ops"))));

      assertThat(body).isEqualTo("ROLE_Admin,ROLE_ops");
    }
  }

  @Test
  void should_read_groups_from_configured_claim() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context =
            start(issuer, "zbar.cognito.groups-claim=custom:groups")) {
      String body =
          authorities(
              context, issuer.token(claims -> claims.claim("custom:groups", List.of("Dev"))));

      assertThat(body).isEqualTo("ROLE_Dev");
    }
  }

  private static ConfigurableApplicationContext start(LocalIssuer issuer, String... properties) {
    return CognitoTestApplication.start(
            "zbar.cognito.issuer-uri=" + issuer.issuer(),
            "zbar.cognito.allowed-client-ids=" + LocalIssuer.CLIENT_ID)
        .properties(properties)
        .sources(AuthoritiesController.class)
        .run();
  }

  private static String authorities(ConfigurableApplicationContext context, String token) {
    return CognitoTestApplication.client(context)
        .get()
        .uri("/authorities")
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .body(String.class);
  }

  @RestController
  static class AuthoritiesController {

    @GetMapping("/authorities")
    String authorities(Authentication authentication) {
      return authentication.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .filter(authority -> authority.startsWith("ROLE_"))
          .sorted()
          .collect(Collectors.joining(","));
    }
  }
}
