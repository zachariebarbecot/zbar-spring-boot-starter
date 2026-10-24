package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class ZbarDockerTest {

  @Test
  void should_fail_with_explicit_message_when_docker_is_unavailable() {
    assertThatIllegalStateException()
        .isThrownBy(() -> ZbarDocker.require(() -> false))
        .withMessageContaining("Docker is required")
        .withMessageContaining("@ZbarSpringBootTest");
  }

  @Test
  void should_pass_when_docker_is_available() {
    assertThatCode(() -> ZbarDocker.require(() -> true)).doesNotThrowAnyException();
  }
}
