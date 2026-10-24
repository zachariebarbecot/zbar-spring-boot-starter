package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@ZbarMockServerTest
@WithoutDataSource
class ZbarMockServerCustomClientIT {

  @Autowired ApplicationContext context;
  @Autowired MockServerClient client;

  @Test
  void should_not_create_the_starter_client_when_the_test_declares_its_own() {
    assertThat(context.getBeansOfType(MockServerClient.class)).containsOnlyKeys("customClient");
    assertThat(client).isSameAs(context.getBean("customClient"));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class CustomClientConfiguration {

    @Bean
    MockServerClient customClient() {
      return mock(MockServerClient.class);
    }
  }
}
