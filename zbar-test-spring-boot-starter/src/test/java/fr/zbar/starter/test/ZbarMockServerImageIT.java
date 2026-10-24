package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.mockserver.MockServerContainer;

@ZbarMockServerTest
@WithoutDataSource
@TestPropertySource(
    properties = "zbar.test.mockserver.image=mockserver/mockserver:mockserver-5.15.0")
class ZbarMockServerImageIT {

  @Autowired MockServerContainer container;

  @Test
  void should_use_configured_image_when_property_is_set() {
    assertThat(container.getDockerImageName()).isEqualTo("mockserver/mockserver:mockserver-5.15.0");
  }
}
