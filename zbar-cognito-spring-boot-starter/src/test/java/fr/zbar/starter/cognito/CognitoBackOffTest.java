package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

class CognitoBackOffTest {

  @Test
  void should_not_register_starter_chain_when_application_declares_its_own() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context =
            CognitoTestApplication.start(
                    "zbar.cognito.issuer-uri=" + issuer.issuer(),
                    "zbar.cognito.allowed-client-ids=" + LocalIssuer.CLIENT_ID)
                .sources(ApplicationChain.class)
                .run()) {
      assertThat(context.containsBean("cognitoSecurityFilterChain")).isFalse();
      assertThat(context.getBeansOfType(SecurityFilterChain.class))
          .containsOnlyKeys("applicationChain");
    }
  }

  @Test
  void should_not_register_any_starter_bean_when_disabled() throws Exception {
    try (LocalIssuer issuer = new LocalIssuer();
        ConfigurableApplicationContext context =
            CognitoTestApplication.run(issuer, "zbar.cognito.enabled=false")) {
      assertThat(context.containsBean("cognitoSecurityFilterChain")).isFalse();
      assertThat(context.containsBean("cognitoJwtDecoder")).isFalse();
      assertThat(context.getBeansOfType(CognitoProperties.class)).isEmpty();
    }
  }

  @Configuration
  static class ApplicationChain {

    @Bean
    SecurityFilterChain applicationChain(HttpSecurity http) {
      return http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).build();
    }
  }
}
