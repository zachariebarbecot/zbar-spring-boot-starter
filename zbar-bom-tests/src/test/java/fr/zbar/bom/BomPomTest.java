package fr.zbar.bom;

import static fr.zbar.bom.PomXml.child;
import static fr.zbar.bom.PomXml.childText;
import static fr.zbar.bom.PomXml.children;
import static fr.zbar.bom.PomXml.managedDependencies;
import static fr.zbar.bom.PomXml.read;
import static fr.zbar.bom.PomXml.texts;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class BomPomTest {

  private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

  @Test
  void rootIsPomAggregatorDeclaringZbarBom() throws Exception {
    Element project = read(ROOT.resolve("pom.xml"));

    assertThat(childText(project, "packaging")).isEqualTo("pom");
    assertThat(texts(child(project, "modules"), "module")).contains("zbar-bom");
  }

  @Test
  void bomHasNoParentIsPomAndImportsThreeBoms() throws Exception {
    Element project = read(ROOT.resolve("zbar-bom/pom.xml"));

    assertThat(child(project, "parent")).isNull();
    assertThat(childText(project, "packaging")).isEqualTo("pom");
    assertThat(importedBoms(project))
        .containsExactlyInAnyOrder(
            "org.springframework.boot:spring-boot-dependencies",
            "org.testcontainers:testcontainers-bom",
            "io.cucumber:cucumber-bom");
  }

  @Test
  void effectivePomManagesVersionOfCoreLibraries() throws Exception {
    Path output = Files.createTempFile("zbar-bom-effective", ".xml");
    Process process =
        new ProcessBuilder(
                ROOT.resolve("mvnw").toString(),
                "-q",
                "-B",
                "-ntp",
                "-f",
                ROOT.resolve("zbar-bom/pom.xml").toString(),
                "help:effective-pom",
                "-Doutput=" + output)
            .redirectErrorStream(true)
            .start();
    String log = new String(process.getInputStream().readAllBytes());
    assertThat(process.waitFor()).as(log).isZero();

    Element managed = child(child(read(output), "dependencyManagement"), "dependencies");
    for (String artifactId :
        List.of(
            "commons-lang3",
            "commons-collections4",
            "testcontainers",
            "cucumber-java",
            "nimbus-jose-jwt")) {
      assertThat(managedVersion(managed, artifactId)).as(artifactId).isNotBlank();
    }
  }

  @Test
  void bomDeclaresNoBouncyCastleEntry() throws Exception {
    assertThat(Files.readString(ROOT.resolve("zbar-bom/pom.xml"))).doesNotContain("org.bouncycastle");
  }

  private static String managedVersion(Element managed, String artifactId) {
    return children(managed, "dependency").stream()
        .filter(dependency -> artifactId.equals(childText(dependency, "artifactId")))
        .map(dependency -> childText(dependency, "version"))
        .findFirst()
        .orElse(null);
  }

  private static List<String> importedBoms(Element project) {
    return managedDependencies(project).stream()
        .filter(
            dependency ->
                "import".equals(childText(dependency, "scope"))
                    && "pom".equals(childText(dependency, "type")))
        .map(dependency -> childText(dependency, "groupId") + ":" + childText(dependency, "artifactId"))
        .toList();
  }
}
