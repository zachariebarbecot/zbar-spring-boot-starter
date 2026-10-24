package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

@ZbarMockServerTest
@WithoutDataSource
class ZbarMockServerClientIT {

  @Autowired MockServerClient mockServer;

  @Value("${zbar.test.mockserver.url}")
  String url;

  @Test
  void should_return_stubbed_response_when_rest_client_calls_expected_path() {
    mockServer.when(request().withMethod("GET").withPath("/x")).respond(response().withBody("hi"));

    String body = RestClient.create(url).get().uri("/x").retrieve().body(String.class);

    assertThat(body).isEqualTo("hi");
  }
}
