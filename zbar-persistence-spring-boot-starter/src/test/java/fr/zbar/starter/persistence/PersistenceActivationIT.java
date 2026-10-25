package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ConfigurableApplicationContext;

class PersistenceActivationIT {

  @ParameterizedTest
  @ValueSource(
      strings = {"--zbar.persistence.enabled=false", "--zbar.persistence.jpa.enabled=false"})
  void should_add_no_jpa_default_when_disabled(String switchOff) {
    try (ConfigurableApplicationContext context = PersistenceTestApplication.run(switchOff)) {
      var environment = context.getEnvironment();

      assertThat(environment.getProperty("spring.jpa.open-in-view")).isNull();
      assertThat(environment.getProperty("spring.jpa.hibernate.naming.physical-strategy")).isNull();
      assertThat(environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size"))
          .isNull();
    }
  }
}
