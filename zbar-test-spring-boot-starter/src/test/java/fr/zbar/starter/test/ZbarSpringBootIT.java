package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

@ZbarSpringBootTest
class ZbarSpringBootIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired MockServerClient mockServer;

  @Value("${zbar.test.mockserver.url}")
  String url;

  @Test
  void should_wire_postgres_and_mockserver_when_context_starts() {
    mockServer.when(request().withPath("/x")).respond(response().withBody("hi"));

    assertThat(jdbc.queryForObject("select 1", Integer.class)).isEqualTo(1);
    assertThat(RestClient.create(url).get().uri("/x").retrieve().body(String.class))
        .isEqualTo("hi");
  }
}
