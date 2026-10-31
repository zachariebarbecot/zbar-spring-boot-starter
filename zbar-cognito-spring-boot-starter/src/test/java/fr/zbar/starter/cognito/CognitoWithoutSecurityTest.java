package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class CognitoWithoutSecurityTest {

  @Test
  void should_start_without_starter_beans_when_spring_security_is_absent() {
    new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CognitoAutoConfiguration.class))
        .withClassLoader(new FilteredClassLoader("org.springframework.security"))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean("cognitoSecurityFilterChain");
            });
  }
}
