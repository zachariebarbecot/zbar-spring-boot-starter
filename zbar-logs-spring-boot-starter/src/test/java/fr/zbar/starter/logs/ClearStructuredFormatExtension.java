package fr.zbar.starter.logs;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Removes the system property Spring Boot sets when a test enables structured console logging. */
class ClearStructuredFormatExtension implements AfterEachCallback {

  @Override
  public void afterEach(ExtensionContext context) {
    System.clearProperty("CONSOLE_LOG_STRUCTURED_FORMAT");
  }
}
