package fr.zbar.springboot.starter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class PackagedJarIT {

  @Test
  void jarContainsGitPropertiesAndBuildInfo() throws IOException {
    try (JarFile jar = new JarFile(packagedJar().toFile())) {
      Properties git = load(jar, "BOOT-INF/classes/git.properties");
      assertThat(git.getProperty("git.commit.id")).isNotBlank();
      assertThat(jar.getEntry("META-INF/build-info.properties")).isNotNull();
    }
  }

  static Path packagedJar() throws IOException {
    try (var jars = Files.newDirectoryStream(Path.of("target"), "zbar-parent-sample-*.jar")) {
      return jars.iterator().next();
    }
  }

  private static Properties load(JarFile jar, String entry) throws IOException {
    var jarEntry = jar.getEntry(entry);
    assertThat(jarEntry).as(entry).isNotNull();
    Properties properties = new Properties();
    properties.load(jar.getInputStream(jarEntry));
    return properties;
  }
}
