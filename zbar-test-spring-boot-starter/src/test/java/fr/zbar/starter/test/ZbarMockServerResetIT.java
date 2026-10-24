package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;

@ZbarMockServerTest
@WithoutDataSource
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ZbarMockServerResetIT {

  @Autowired MockServerClient mockServer;

  @Test
  @Order(1)
  void should_define_an_expectation() {
    mockServer.when(request().withPath("/x")).respond(response().withBody("hi"));

    assertThat(mockServer.retrieveActiveExpectations(request())).hasSize(1);
  }

  @Test
  @Order(2)
  void should_reset_expectations_when_a_new_test_starts() {
    assertThat(mockServer.retrieveActiveExpectations(request())).isEmpty();
  }
}
