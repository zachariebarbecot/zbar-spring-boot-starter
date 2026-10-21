package fr.zbar.bom;

import static fr.zbar.bom.PomXml.child;
import static fr.zbar.bom.PomXml.childText;
import static fr.zbar.bom.PomXml.children;
import static fr.zbar.bom.PomXml.effectivePom;
import static fr.zbar.bom.PomXml.importedBoms;
import static fr.zbar.bom.PomXml.ROOT;
import static fr.zbar.bom.PomXml.read;
import static fr.zbar.bom.PomXml.texts;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class BomPomTest {

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
            "org.springframework.boot:spring-boot-dependencies:${spring-boot.version}",
            "org.testcontainers:testcontainers-bom:${testcontainers.version}",
            "io.cucumber:cucumber-bom:${cucumber.version}");
  }

  @Test
  void effectivePomManagesVersionOfCoreLibraries() throws Exception {
    Element managed = child(child(effectivePom("zbar-bom"), "dependencyManagement"), "dependencies");
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
}
