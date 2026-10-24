package fr.zbar.starter.test;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.client.MockServerClient;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class ZbarMockServerResetExtension implements BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    SpringExtension.getApplicationContext(context).getBean(MockServerClient.class).reset();
  }
}
