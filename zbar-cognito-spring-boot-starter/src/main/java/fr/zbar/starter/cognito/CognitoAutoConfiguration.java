package fr.zbar.starter.cognito;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@ConditionalOnProperty(prefix = "zbar.cognito", name = "enabled", matchIfMissing = true)
@AutoConfiguration(
    beforeName = {
      "org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration",
      "org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration",
      "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration"
    })
@EnableConfigurationProperties(CognitoProperties.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class CognitoAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  JwtDecoder cognitoJwtDecoder(CognitoProperties properties) {
    String issuer = properties.issuerUri();
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSetUri(issuer + "/.well-known/jwks.json").build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return decoder;
  }

  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  SecurityFilterChain cognitoSecurityFilterChain(HttpSecurity http) {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }
}
