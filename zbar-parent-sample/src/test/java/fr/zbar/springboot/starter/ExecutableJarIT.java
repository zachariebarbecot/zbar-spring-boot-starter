package fr.zbar.springboot.starter;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ExecutableJarIT {

  @Test
  void packagedJarStartsTheApplication() throws IOException, InterruptedException {
    Path java = Path.of(System.getProperty("java.home"), "bin", "java");
    Process process =
        new ProcessBuilder(java.toString(), "-jar", PackagedJarIT.packagedJar().toString())
            .redirectErrorStream(true)
            .start();

    boolean exited = process.waitFor(60, SECONDS);
    if (!exited) {
      process.destroyForcibly();
    }
    String output = new String(process.getInputStream().readAllBytes());

    assertThat(exited).as(output).isTrue();
    assertThat(process.exitValue()).as(output).isZero();
    assertThat(output).contains("Started ZbarSpringBootStarterApplication");
  }
}
