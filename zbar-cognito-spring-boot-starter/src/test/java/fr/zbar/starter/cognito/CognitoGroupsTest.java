package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

  @Test
  void should_allow_endpoint_requiring_role_for_token_of_that_group() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context = start(issuer)) {
      int status =
          status(
              context,
              "/admin",
              issuer.token(claims -> claims.claim("cognito:groups", List.of("Admin"))));

      assertThat(status).isEqualTo(200);
    }
  }

  @Test
  void should_forbid_endpoint_requiring_role_for_token_without_that_group() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context = start(issuer)) {
      int status =
          status(
              context,
              "/admin",
              issuer.token(claims -> claims.claim("cognito:groups", List.of("ops"))));

      assertThat(status).isEqualTo(403);
    }
  }

  @Test
  void should_authenticate_token_without_groups_with_no_role() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context = start(issuer)) {
      String token = issuer.accessToken();

      assertThat(status(context, "/authorities", token)).isEqualTo(200);
      assertThat(authorities(context, token)).isNullOrEmpty();
    }
  }

  private static int status(ConfigurableApplicationContext context, String path, String token) {
    return CognitoTestApplication.client(context)
        .get()
        .uri(path)
        .headers(headers -> headers.setBearerAuth(token))
        .exchange((request, response) -> response.getStatusCode().value());
  }

  private static ConfigurableApplicationContext start(LocalIssuer issuer, String... properties) {
    return CognitoTestApplication.start(
            "zbar.cognito.issuer-uri=" + issuer.issuer(),
            "zbar.cognito.allowed-client-ids=" + LocalIssuer.CLIENT_ID)
        .properties(properties)
        .sources(AuthoritiesController.class, MethodSecurity.class)
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

  @Configuration
  @EnableMethodSecurity
  static class MethodSecurity {}

  @RestController
  static class AuthoritiesController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('Admin')")
    String admin() {
      return "admin";
    }

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
