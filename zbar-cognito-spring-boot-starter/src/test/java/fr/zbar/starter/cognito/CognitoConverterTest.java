package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class CognitoConverterTest {

  @Test
  void should_use_application_converter_authorities() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context =
            CognitoTestApplication.start(
                    "zbar.cognito.issuer-uri=" + issuer.issuer(),
                    "zbar.cognito.allowed-client-ids=" + LocalIssuer.CLIENT_ID)
                .sources(ApplicationConverter.class, SpecialController.class)
                .run()) {
      int status =
          CognitoTestApplication.client(context)
              .get()
              .uri("/special")
              .headers(headers -> headers.setBearerAuth(issuer.accessToken()))
              .exchange((request, response) -> response.getStatusCode().value());

      assertThat(status).isEqualTo(200);
    }
  }

  @Configuration
  @EnableMethodSecurity
  static class ApplicationConverter {

    @Bean
    JwtAuthenticationConverter applicationConverter() {
      JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
      converter.setJwtGrantedAuthoritiesConverter(
          jwt -> List.of(new SimpleGrantedAuthority("APPLICATION_AUTHORITY")));
      return converter;
    }
  }

  @RestController
  static class SpecialController {

    @GetMapping("/special")
    @PreAuthorize("hasAuthority('APPLICATION_AUTHORITY')")
    String special() {
      return "special";
    }
  }
}
