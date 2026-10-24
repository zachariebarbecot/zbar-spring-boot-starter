package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@ZbarMockServerTest
@WithoutDataSource
class ZbarMockServerIT {

  @Autowired Environment environment;

  @Test
  void should_resolve_url_host_and_port_when_context_starts() {
    String host = environment.getProperty("zbar.test.mockserver.host");
    Integer port = environment.getProperty("zbar.test.mockserver.port", Integer.class);

    assertThat(host).isNotBlank();
    assertThat(port).isPositive();
    assertThat(environment.getProperty("zbar.test.mockserver.url"))
        .isEqualTo("http://" + host + ":" + port);
  }
}
