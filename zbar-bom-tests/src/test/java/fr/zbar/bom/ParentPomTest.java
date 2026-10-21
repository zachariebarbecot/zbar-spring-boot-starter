package fr.zbar.bom;

import static fr.zbar.bom.PomXml.child;
import static fr.zbar.bom.PomXml.childText;
import static fr.zbar.bom.PomXml.children;
import static fr.zbar.bom.PomXml.managedDependencies;
import static fr.zbar.bom.PomXml.read;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class ParentPomTest {

  private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
  private static final Path PARENT_POM = ROOT.resolve("zbar-parent/pom.xml");
  private static final Path SAMPLE = ROOT.resolve("zbar-parent-sample");

  @Test
  void parentInheritsSpringBootStarterParentAndImportsZbarBom() throws Exception {
    Element project = readExisting(PARENT_POM);

    Element parent = child(project, "parent");
    assertThat(childText(parent, "groupId")).isEqualTo("org.springframework.boot");
    assertThat(childText(parent, "artifactId")).isEqualTo("spring-boot-starter-parent");
    assertThat(childText(parent, "version")).isEqualTo("4.1.1");
    assertThat(importedBoms(project)).contains("fr.zbar:zbar-bom:${project.version}");
  }

  @Test
  void parentSetsOverridableJava25() throws Exception {
    Element project = readExisting(PARENT_POM);

    assertThat(childText(child(project, "properties"), "java.version")).isEqualTo("25");
  }

  @Test
  void sampleInheritsParentAndDeclaresManagedDependencyWithoutVersion() throws Exception {
    Element project = readExisting(SAMPLE.resolve("pom.xml"));

    assertThat(childText(child(project, "parent"), "artifactId")).isEqualTo("zbar-parent");
    Element dependency = dependency(project, "commons-collections4");
    assertThat(dependency).isNotNull();
    assertThat(child(dependency, "version")).isNull();
  }

  @Test
  void applicationAndTestMovedFromRootToSample() {
    assertThat(ROOT.resolve("src")).doesNotExist();
    assertThat(SAMPLE.resolve("src/main/java/fr/zbar/springboot/starter"))
        .isDirectoryContaining("glob:**ZbarSpringBootStarterApplication.java");
    assertThat(SAMPLE.resolve("src/test/java/fr/zbar/springboot/starter"))
        .isDirectoryContaining("glob:**ZbarSpringBootStarterApplicationTests.java");
  }

  private static Element readExisting(Path pom) throws Exception {
    assertThat(pom).exists();
    return read(pom);
  }

  private static Element dependency(Element project, String artifactId) {
    return children(child(project, "dependencies"), "dependency").stream()
        .filter(dependency -> artifactId.equals(childText(dependency, "artifactId")))
        .findFirst()
        .orElse(null);
  }

  private static List<String> importedBoms(Element project) {
    return managedDependencies(project).stream()
        .filter(dependency -> "import".equals(childText(dependency, "scope")))
        .map(
            dependency ->
                childText(dependency, "groupId")
                    + ":"
                    + childText(dependency, "artifactId")
                    + ":"
                    + childText(dependency, "version"))
        .toList();
  }
}
