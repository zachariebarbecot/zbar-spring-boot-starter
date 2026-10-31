package fr.zbar.starter.cognito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;

class CognitoStartupFailureTest {

  @Test
  void should_fail_naming_issuer_uri_when_no_issuer_is_configured() {
    assertThat(failureMessage("zbar.cognito.allowed-client-ids=c"))
        .contains("zbar.cognito.issuer-uri");
  }

  @Test
  void should_fail_naming_user_pool_id_when_only_region_is_configured() {
    assertThat(failureMessage("zbar.cognito.allowed-client-ids=c", "zbar.cognito.region=eu-west-1"))
        .contains("zbar.cognito.user-pool-id");
  }

  @Test
  void should_fail_naming_allowed_client_ids_when_none_is_configured() {
    assertThat(failureMessage("zbar.cognito.issuer-uri=https://issuer.example.com/pool"))
        .contains("zbar.cognito.allowed-client-ids");
  }

  @Test
  void should_start_when_issuer_is_derived_from_region_and_user_pool() {
    try (ConfigurableApplicationContext context =
        CognitoTestApplication.start(
                "zbar.cognito.allowed-client-ids=c",
                "zbar.cognito.region=eu-west-1",
                "zbar.cognito.user-pool-id=eu-west-1_abc")
            .run()) {
      assertThat(context.isRunning()).isTrue();
    }
  }

  private static String failureMessage(String... properties) {
    Throwable failure = catchThrowable(() -> CognitoTestApplication.start(properties).run());
    return NestedExceptionUtils.getMostSpecificCause(failure).getMessage();
  }
}
