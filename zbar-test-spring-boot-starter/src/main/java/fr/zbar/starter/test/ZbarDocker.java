package fr.zbar.starter.test;

import java.util.function.BooleanSupplier;

final class ZbarDocker {

  private ZbarDocker() {}

  static void require(BooleanSupplier dockerAvailable) {
    if (!dockerAvailable.getAsBoolean()) {
      throw new IllegalStateException(
          "Docker is required to run @ZbarSpringBootTest, @ZbarPostgresTest and"
              + " @ZbarMockServerTest: start Docker and retry");
    }
  }
}
